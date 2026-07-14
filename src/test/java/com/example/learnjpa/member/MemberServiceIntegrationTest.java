package com.example.learnjpa.member;

import com.example.learnjpa.member.exception.DuplicateEmailException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class MemberServiceIntegrationTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberService memberService;

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시도로 인해 예외 발생")
    void signup_duplicate_email_throw() {
        String email = "test@example.com";

        Member existMember = Member.builder()
                .name("name1")
                .email(email)
                .build();
        memberRepository.save(existMember);
    }
}
