package com.example.learnjpa.member.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record MemberResponse(
        Long id,
        String name,
        String email
) {
}
