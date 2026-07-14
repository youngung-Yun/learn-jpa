package com.example.learnjpa.member;

import com.example.learnjpa.member.dto.request.SignupRequest;
import com.example.learnjpa.member.dto.response.MemberResponse;
import com.example.learnjpa.member.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberResponse getById(Long id) {
        var member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "유효하지 않은 멤버 id입니다: %d".formatted(id)
                ));
        return toResponse(member);
    }

    @Transactional
    public Long signup(SignupRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        var saved = memberRepository.save(toEntity(request));
        return saved.getId();
    }

    private Member toEntity(SignupRequest request) {
        return Member.builder()
                .name(request.name())
                .email(request.email())
                .build();
    }

    private MemberResponse toResponse(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .build();
    }
}