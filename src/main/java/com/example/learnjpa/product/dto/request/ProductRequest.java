package com.example.learnjpa.product.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductRequest(
        @NotNull
        @Positive
        Long id
) {
}
