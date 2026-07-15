package com.example.learnjpa.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRegisterRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @PositiveOrZero
        Long price,

        @NotNull
        @PositiveOrZero
        Long stockQuantity
) {
}
