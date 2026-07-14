package com.example.learnjpa.order.dto.response;

import com.example.learnjpa.order.Order;
import com.example.learnjpa.order.OrderProduct;
import com.example.learnjpa.order.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderDetailsResponse(
        Long orderId,
        OrderStatus status,
        Instant orderedAt,
        MemberDetails member,
        List<OrderProductDetails> products,
        Long totalPrice
) {

    public static OrderDetailsResponse from(Order order) {
        List<OrderProductDetails> products = order.getOrderProductList()
                .stream()
                .map(OrderProductDetails::from)
                .toList();

        long totalPrice = products.stream()
                .mapToLong(OrderProductDetails::totalPrice)
                .sum();

        return new OrderDetailsResponse(
                order.getId(),
                order.getStatus(),
                order.getCreatedAt(),
                MemberDetails.from(order),
                products,
                totalPrice
        );
    }

    public record MemberDetails(
            Long memberId,
            String name
    ) {
        private static MemberDetails from(Order order) {
            return new MemberDetails(
                    order.getMember().getId(),
                    order.getMember().getName()
            );
        }
    }

    public record OrderProductDetails(
            Long orderProductId,
            Long productId,
            String productName,
            Long orderPrice,
            Long quantity,
            Long totalPrice
    ) {
        private static OrderProductDetails from(OrderProduct orderProduct) {
            long totalPrice = orderProduct.getOrderPrice() * orderProduct.getQuantity();

            return new OrderProductDetails(
                    orderProduct.getId(),
                    orderProduct.getProduct().getId(),
                    orderProduct.getProduct().getName(),
                    orderProduct.getOrderPrice(),
                    orderProduct.getQuantity(),
                    totalPrice
            );
        }
    }
}
