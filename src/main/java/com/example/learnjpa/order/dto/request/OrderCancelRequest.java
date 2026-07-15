package com.example.learnjpa.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderCancelRequest(
        @NotNull
        @Positive
        Long orderId
) {
}
