package com.example.learnjpa.order;

import com.example.learnjpa.member.MemberRepository;
import com.example.learnjpa.order.dto.request.OrderCancelRequest;
import com.example.learnjpa.order.dto.request.OrderCreateRequest;
import com.example.learnjpa.order.dto.request.OrderDeliverRequest;
import com.example.learnjpa.order.dto.response.OrderDetailsResponse;
import com.example.learnjpa.order.repository.OrderRepository;
import com.example.learnjpa.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final ProductRepository productRepository;

    private final MemberRepository memberRepository;

    @Transactional
    public Long createOrder(OrderCreateRequest request) {

        for (var createOrderProduct : request.createOrderProductList()) {
            if (createOrderProduct.quantity() == 0) {
                throw new IllegalArgumentException("주문 수량은 0보다 많아야 합니다. productId: %d".formatted(createOrderProduct.productId()));
            }
        }

        // memberId로 Member 엔티티 가져옴
        var memberId = request.memberId();
        var member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 Member id입니다: %d".formatted(memberId)));

        // 가져온 Member로 Order 엔티티 생성하여 저장
        var order = new Order(member);
        var savedOrder = orderRepository.save(order);

        // 각 request에 대해 Product 엔티티 가져와 수량 감소 후 OrderProduct 엔티티 생성
        for (var createOrderProduct : request.createOrderProductList()) {
            var productId = createOrderProduct.productId();
            var product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 Product id입니다: %d".formatted(productId)));
            var quantity = createOrderProduct.quantity();

            product.decreaseStockQuantity(quantity);

            OrderProduct.builder()
                    .order(savedOrder)
                    .product(product)
                    .orderPrice(product.getPrice())
                    .quantity(quantity)
                    .build();
        }

        return savedOrder.getId();
    }

    @Transactional
    public void cancel(OrderCancelRequest request) {
        var order = orderRepository.findById(request.orderId())
                .orElseThrow(
                        () -> new IllegalArgumentException("유효하지 않은 Order id입니다: %d".formatted(request.orderId())));
        order.cancel();

        // 재고 복구
        for (var orderProduct : order.getOrderProductList()) {
            var product = orderProduct.getProduct();
            var quantity = orderProduct.getQuantity();
            product.increaseStockQuantity(quantity);
        }
    }

    @Transactional
    public void deliver(OrderDeliverRequest request) {
        var order = orderRepository.findById(request.orderId())
                .orElseThrow(
                        () -> new IllegalArgumentException("유효하지 않은 Order id입니다: %d".formatted(request.orderId())));
        order.deliver();
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderDetailsNormally(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow();

        return OrderDetailsResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderDetailsWithFetchJoin(Long orderId) {
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow();

        return OrderDetailsResponse.from(order);
    }
}
