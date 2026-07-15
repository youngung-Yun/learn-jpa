package com.example.learnjpa.order;

import com.example.learnjpa.config.JpaConfig;
import com.example.learnjpa.member.Member;
import com.example.learnjpa.member.MemberRepository;
import com.example.learnjpa.order.dto.request.OrderCancelRequest;
import com.example.learnjpa.order.dto.request.OrderCreateRequest;
import com.example.learnjpa.order.dto.response.OrderDetailsResponse;
import com.example.learnjpa.order.exception.InvalidOrderStatusException;
import com.example.learnjpa.order.repository.OrderRepository;
import com.example.learnjpa.product.Product;
import com.example.learnjpa.product.exception.InsufficientStockException;
import com.example.learnjpa.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@Import({JpaConfig.class, OrderService.class})
class OrderServiceIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("주문 생성 성공")
    void create_order_success() {
        String productName = "product";
        Long productPrice = 10_000L;
        Long productQuantity = 100L;
        Long orderQuantity = 50L;

        var savedMember = createAndSaveMember("member", "test@example.com");
        var savedProduct = createAndSaveProduct(productName, productPrice, productQuantity);

        entityManager.clear();

        var productId = savedProduct.getId();
        var memberId = savedMember.getId();

        var savedOrder = createAndFindOrder(memberId, productId, orderQuantity);

        // product 검증
        var foundProduct = productRepository.findById(productId)
                        .orElseThrow();
        assertThat(foundProduct.getName()).isEqualTo(savedProduct.getName());
        assertThat(foundProduct.getPrice()).isEqualTo(savedProduct.getPrice());
        assertThat(foundProduct.getStockQuantity()).isEqualTo(productQuantity - orderQuantity);

        // order 검증
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(savedOrder.getOrderProductList()).hasSize(1);

        // orderProduct 검증
        var savedOrderProduct = savedOrder.getOrderProductList().getFirst();
        assertThat(savedOrderProduct.getProduct().getId()).isEqualTo(productId);
        assertThat(savedOrderProduct.getOrderPrice()).isEqualTo(productPrice);
        assertThat(savedOrderProduct.getQuantity()).isEqualTo(orderQuantity);
    }

    @Test
    @DisplayName("이미 취소한 주문 취소 시도시 예외 발생")
    void cancel_order_again_throw() {
        Long stockQuantity = 100L;
        Long orderQuantity = 50L;

        var memberId = createAndSaveMember("member", "test@example.com").getId();
        var savedProduct = createAndSaveProduct("product", 10_000L, stockQuantity);

        var savedOrder = createAndFindOrder(memberId, savedProduct.getId(), orderQuantity);

        // 취소
        orderService.cancel(new OrderCancelRequest(savedOrder.getId()));
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        var foundProduct = productRepository.findById(savedProduct.getId())
                        .orElseThrow();
        assertThat(foundProduct.getStockQuantity()).isEqualTo(stockQuantity);

        // 재취소
        var foundOrder = orderRepository.findById(savedOrder.getId())
                .orElseThrow();
        assertThatThrownBy(() -> orderService.cancel(new OrderCancelRequest(foundOrder.getId())))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("주문 중 재고 부족한 제품 있으면 주문 전체 롤백")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(
            statements = {
                    "DELETE FROM order_products",
                    "DELETE FROM orders",
                    "DELETE FROM products",
                    "DELETE FROM members"
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void cancel_order_insufficient_stock_rollback () {
        Long product1Quantity = 100L;
        Long product2Quantity = 3L;
        Long orderQuantity = 50L;

        var memberId = createAndSaveMember("member", "test@example.com").getId();
        var product1 = createAndSaveProduct("product1", 10_000L, product1Quantity);
        var product2 = createAndSaveProduct("product2", 10_000L, product2Quantity);

        entityManager.clear();

        var request = new OrderCreateRequest(
                memberId,
                List.of(
                        new OrderCreateRequest.OrderCreateProductRequest(
                                product1.getId(), orderQuantity
                        ),
                        new OrderCreateRequest.OrderCreateProductRequest(
                                product2.getId(), orderQuantity
                        )
                )
        );

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class);

        var foundProduct1 = productRepository.findById(product1.getId())
                .orElseThrow();
        assertThat(foundProduct1.getStockQuantity()).isEqualTo(product1Quantity);
        var foundProduct2 = productRepository.findById(product2.getId())
                .orElseThrow();
        assertThat(foundProduct2.getStockQuantity()).isEqualTo(product2Quantity);
    }

    /*
    2026-07-15T01:17:33.110+09:00 DEBUG 27048 --- [learn-jpa] [    Test worker] org.hibernate.SQL                        :
    select
        o1_0.id,
        o1_0.ordered_at,
        o1_0.member_id,
        o1_0.status,
        o1_0.updated_at
    from
        orders o1_0
    where
        o1_0.id=?
2026-07-15T01:17:33.135+09:00 DEBUG 27048 --- [learn-jpa] [    Test worker] org.hibernate.SQL                        :
    select
        opl1_0.order_id,
        opl1_0.id,
        opl1_0.order_price,
        opl1_0.product_id,
        opl1_0.quantity
    from
        order_products opl1_0
    where
        opl1_0.order_id=?
2026-07-15T01:17:33.139+09:00 DEBUG 27048 --- [learn-jpa] [    Test worker] org.hibernate.SQL                        :
    select
        p1_0.id,
        p1_0.created_at,
        p1_0.name,
        p1_0.price,
        p1_0.stock_quantity,
        p1_0.updated_at
    from
        products p1_0
    where
        p1_0.id=?
2026-07-15T01:17:33.141+09:00 DEBUG 27048 --- [learn-jpa] [    Test worker] org.hibernate.SQL                        :
    select
        p1_0.id,
        p1_0.created_at,
        p1_0.name,
        p1_0.price,
        p1_0.stock_quantity,
        p1_0.updated_at
    from
        products p1_0
    where
        p1_0.id=?
2026-07-15T01:17:33.142+09:00 DEBUG 27048 --- [learn-jpa] [    Test worker] org.hibernate.SQL                        :
    select
        p1_0.id,
        p1_0.created_at,
        p1_0.name,
        p1_0.price,
        p1_0.stock_quantity,
        p1_0.updated_at
    from
        products p1_0
    where
        p1_0.id=?
2026-07-15T01:17:33.145+09:00 DEBUG 27048 --- [learn-jpa] [    Test worker] org.hibernate.SQL                        :
    select
        m1_0.id,
        m1_0.created_at,
        m1_0.email,
        m1_0.name,
        m1_0.updated_at
    from
        members m1_0
    where
        m1_0.id=?

     쿼리 6번 조회
     ---
     2026-07-15T10:31:33.083+09:00 DEBUG 11524 --- [    Test worker] org.hibernate.SQL                        :
    select
        o1_0.id,
        o1_0.ordered_at,
        o1_0.member_id,
        o1_0.status,
        o1_0.updated_at
    from
        orders o1_0
    where
        o1_0.id=?
2026-07-15T10:31:33.121+09:00 DEBUG 11524 --- [    Test worker] org.hibernate.SQL                        :
    select
        opl1_0.order_id,
        opl1_0.id,
        opl1_0.order_price,
        opl1_0.product_id,
        opl1_0.quantity
    from
        order_products opl1_0
    where
        opl1_0.order_id=?
2026-07-15T10:31:33.132+09:00 DEBUG 11524 --- [    Test worker] org.hibernate.SQL                        :
    select
        p1_0.id,
        p1_0.created_at,
        p1_0.name,
        p1_0.price,
        p1_0.stock_quantity,
        p1_0.updated_at
    from
        products p1_0
    where
        p1_0.id in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
2026-07-15T10:31:33.144+09:00 DEBUG 11524 --- [    Test worker] org.hibernate.SQL                        :
    select
        m1_0.id,
        m1_0.created_at,
        m1_0.email,
        m1_0.name,
        m1_0.updated_at
    from
        members m1_0
    where
        m1_0.id=?
     @BatchSize : 쿼리 4번 실행
     */
    @Test
    @DisplayName("일반 주문 상세 조회")
    void general_find_order_details() {
        Long orderId = createOrderWithSeveralProducts();

        entityManager.flush();
        entityManager.clear();

        OrderDetailsResponse response =
                orderService.getOrderDetailsNormally(orderId);

        assertThat(response.products()).hasSize(3);
    }
    /*
    2026-07-15T01:16:54.063+09:00 DEBUG 27892 --- [learn-jpa] [    Test worker] org.hibernate.SQL                        :
    select
        distinct o1_0.id,
        o1_0.ordered_at,
        o1_0.member_id,
        m1_0.id,
        m1_0.created_at,
        m1_0.email,
        m1_0.name,
        m1_0.updated_at,
        opl1_0.order_id,
        opl1_0.id,
        opl1_0.order_price,
        opl1_0.product_id,
        p1_0.id,
        p1_0.created_at,
        p1_0.name,
        p1_0.price,
        p1_0.stock_quantity,
        p1_0.updated_at,
        opl1_0.quantity,
        o1_0.status,
        o1_0.updated_at
    from
        orders o1_0
    join
        members m1_0
            on m1_0.id=o1_0.member_id
    join
        order_products opl1_0
            on o1_0.id=opl1_0.order_id
    join
        products p1_0
            on p1_0.id=opl1_0.product_id
    where
        o1_0.id=?

     쿼리 1회 실행
     */
    @Test
    @DisplayName("Fetch Join 주문 상세 조회")
    void fetch_join_find_order_details() {
        Long orderId = createOrderWithSeveralProducts();

        entityManager.flush();
        entityManager.clear();

        OrderDetailsResponse response =
                orderService.getOrderDetailsWithFetchJoin(orderId);

        assertThat(response.products()).hasSize(3);
    }

    /*
    2026-07-15T10:25:35.614+09:00 DEBUG 29264 --- [    Test worker] org.hibernate.SQL                        :
    select
        distinct o1_0.id,
        o1_0.ordered_at,
        o1_0.member_id,
        m1_0.id,
        m1_0.created_at,
        m1_0.email,
        m1_0.name,
        m1_0.updated_at,
        opl1_0.order_id,
        opl1_0.id,
        opl1_0.order_price,
        opl1_0.product_id,
        p1_0.id,
        p1_0.created_at,
        p1_0.name,
        p1_0.price,
        p1_0.stock_quantity,
        p1_0.updated_at,
        opl1_0.quantity,
        o1_0.status,
        o1_0.updated_at
    from
        orders o1_0
    join
        members m1_0
            on m1_0.id=o1_0.member_id
    join
        order_products opl1_0
            on o1_0.id=opl1_0.order_id
    join
        products p1_0
            on p1_0.id=opl1_0.product_id
    where
        o1_0.id=?
    쿼리 1회 실행
     */
    @Test
    @DisplayName("Entity Graph 주문 상세 조회")
    void entity_graph_find_order_details() {
        Long orderId = createOrderWithSeveralProducts();

        entityManager.flush();
        entityManager.clear();

        OrderDetailsResponse response =
                orderService.getOrderDetailsWithEntityGraph(orderId);

        assertThat(response.products()).hasSize(3);
    }

    private Member createAndSaveMember(String name, String email) {
        var member = Member.builder()
                .name(name)
                .email(email)
                .build();

        return memberRepository.saveAndFlush(member);
    }

    private Product createAndSaveProduct(String name, Long price, Long quantity) {
        var product = Product.builder()
                .name(name)
                .price(price)
                .stockQuantity(quantity)
                .build();

        return productRepository.saveAndFlush(product);
    }

    private Order createAndFindOrder(Long memberId, Long productId, Long orderQuantity) {
        var orderCreateRequest = new OrderCreateRequest(memberId,
                List.of(
                        new OrderCreateRequest.OrderCreateProductRequest(productId, orderQuantity)
                ));

        Long orderId = orderService.createOrder(orderCreateRequest);

        entityManager.flush();
        entityManager.clear();

        return orderRepository.findById(orderId)
                .orElseThrow();
    }

    private Long createOrderWithSeveralProducts() {
        var member = createAndSaveMember(
                "member",
                "several-products@example.com"
        );

        var keyboard = createAndSaveProduct(
                "키보드",
                50_000L,
                10L
        );

        var mouse = createAndSaveProduct(
                "마우스",
                30_000L,
                10L
        );

        var monitor = createAndSaveProduct(
                "모니터",
                200_000L,
                10L
        );

        var request = new OrderCreateRequest(
                member.getId(),
                List.of(
                        new OrderCreateRequest.OrderCreateProductRequest(
                                keyboard.getId(),
                                2L
                        ),
                        new OrderCreateRequest.OrderCreateProductRequest(
                                mouse.getId(),
                                1L
                        ),
                        new OrderCreateRequest.OrderCreateProductRequest(
                                monitor.getId(),
                                1L
                        )
                )
        );

        return orderService.createOrder(request);
    }
}
