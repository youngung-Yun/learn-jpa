package com.example.learnjpa.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderDeliverRequest(
        @NotNull
        @Positive
        Long orderId
) {
}
