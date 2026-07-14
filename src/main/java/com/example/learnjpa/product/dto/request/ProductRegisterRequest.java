package com.example.learnjpa.product.dto.request;

public record ProductRegisterRequest(
        String name,
        Long price,
        Long stockQuantity
) {
}
