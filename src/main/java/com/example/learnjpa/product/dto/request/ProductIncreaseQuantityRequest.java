package com.example.learnjpa.product.dto.request;

public record ProductIncreaseQuantityRequest(
        Long productId,
        Long increaseQuantity
) {
}
