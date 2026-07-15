package com.example.learnjpa.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderCreateRequest(
        @NotNull
        @Positive
        Long memberId,

        @NotEmpty
        List<@Valid OrderCreateProductRequest> createOrderProductList
) {

    public record OrderCreateProductRequest(
            @NotNull
            @Positive
            Long productId,

            @NotNull
            @Positive
            Long quantity
    ) {
    }
}
