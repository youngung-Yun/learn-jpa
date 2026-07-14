package com.example.learnjpa.member;

import com.example.learnjpa.member.dto.request.SignupRequest;
import com.example.learnjpa.member.exception.DuplicateEmailException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;

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
}