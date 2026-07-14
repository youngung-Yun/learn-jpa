package com.example.learnjpa.order.dto.request;

import java.util.List;

public record OrderCreateRequest(
        Long memberId,
        List<OrderCreateProductRequest> createOrderProductList
) {

    public record OrderCreateProductRequest(
            Long productId,
            Long quantity
    ) {
    }
}
