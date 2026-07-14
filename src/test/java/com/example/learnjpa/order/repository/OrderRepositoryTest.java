package com.example.learnjpa.order.repository;

import com.example.learnjpa.config.JpaConfig;
import com.example.learnjpa.member.Member;
import com.example.learnjpa.order.Order;
import com.example.learnjpa.order.OrderProduct;
import com.example.learnjpa.order.OrderStatus;
import com.example.learnjpa.product.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@Import(JpaConfig.class)
class OrderRepositoryTest {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("Order 매핑 테스트")
    void save_test() {
        var member = Member.builder()
                .name("name")
                .email("test@example.com")
                .build();

        entityManager.persist(member);
        var order = new Order(member);

        var saved = orderRepository.save(order);

        entityManager.flush();
        entityManager.clear();

        var found = orderRepository.findById(saved.getId())
                        .orElseThrow();

        assertThat(found.getMember().getId()).isEqualTo(member.getId());
        assertThat(found.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Order 저장 시 OrderProduct도 Cascade로 저장되는지 테스트")
    void saved_order_cascade_orderProduct() {
        var order = createOrder();
        var product = createProduct();

        var orderProduct = OrderProduct.builder()
                .order(order)
                .product(product)
                .orderPrice(10_000L)
                .quantity(100L)
                .build();

        entityManager.persist(product);
        var saved = orderRepository.save(order);

        entityManager.flush();
        entityManager.clear();

        var found = orderRepository.findById(order.getId())
                .orElseThrow();
        var foundOrderProduct = found.getOrderProductList().getFirst();

        assertThat(found.getOrderProductList()).hasSize(1);
        assertThat(foundOrderProduct.getId()).isNotNull();
        assertThat(foundOrderProduct.getOrder().getId())
                .isEqualTo(found.getId());
        assertThat(foundOrderProduct.getProduct().getId())
                .isEqualTo(product.getId());
        assertThat(foundOrderProduct.getOrderPrice())
                .isEqualTo(10_000L);
        assertThat(foundOrderProduct.getQuantity())
                .isEqualTo(100L);
    }

    private Product createProduct() {
        return Product.builder()
                .name("product1")
                .price(10_000L)
                .stockQuantity(100L)
                .build();
    }

    private Order createOrder() {
        var member = Member.builder()
                .name("name")
                .email("test@example.com")
                .build();

        entityManager.persist(member);

        return new Order(member);
    }
}