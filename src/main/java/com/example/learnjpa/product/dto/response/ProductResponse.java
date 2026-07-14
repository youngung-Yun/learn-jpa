package com.example.learnjpa.product.dto.response;

import lombok.Builder;

@Builder
public record ProductResponse(
        Long id,
        String name,
        Long price,
        Long quantity
) {

}
