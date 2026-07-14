package com.example.learnjpa.member;

import com.example.learnjpa.config.JpaConfig;
import com.example.learnjpa.member.dto.request.SignupRequest;
import com.example.learnjpa.member.exception.DuplicateEmailException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@Import({JpaConfig.class, MemberService.class})
class MemberServiceIntegrationTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MemberService memberService;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        var request = new SignupRequest("name", "test@example.com");

        var resultId = memberService.signup(request);

        entityManager.flush();
        entityManager.clear();

        var found = memberRepository.findById(resultId)
                .orElseThrow();

        assertThat(found.getName()).isEqualTo(request.name());
        assertThat(found.getEmail()).isEqualTo(request.email());
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시도로 인해 예외 발생")
    void signup_duplicate_email_throw() {
        String email = "test@example.com";

        var existMember = Member.builder()
                .name("name1")
                .email(email)
                .build();
        memberRepository.saveAndFlush(existMember);
        entityManager.clear();

        var request = new SignupRequest("name2", email);

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);

        assertThat(memberRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Member 조회 성공")
    void find_member_success() {
        var member = Member.builder()
                .name("name")
                .email("test@example.com")
                .build();

        var id = memberRepository.saveAndFlush(member).getId();
        entityManager.clear();

        var found = memberService.getById(id);

        assertThat(found.name()).isEqualTo(member.getName());
        assertThat(found.email()).isEqualTo(member.getEmail());
    }

    @Test
    @DisplayName("존재하지 않는 id로 Member 조회하여 예외 발생")
    void find_member_nonextistent_throw() {
        var id = 1L;

        assertThatThrownBy(() -> memberService.getById(id))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
