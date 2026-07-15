package com.example.learnjpa.product.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductIncreaseQuantityRequest(

        @NotNull
        @Positive
        Long productId,

        @NotNull
        @Positive
        Long increaseQuantity
) {
}
