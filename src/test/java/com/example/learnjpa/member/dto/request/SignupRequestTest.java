package com.example.learnjpa.member.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignupRequestTest {

    private static final String correctName = "name";
    private static final String longName = """
            aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
            aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa""";
    private static final String correctEmail = "test@example.com";

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void cleanup() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("검증 성공")
    void validate_success() {

        var request = new SignupRequest(correctName, correctEmail);

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("name이 null이라 검증 실패")
    void validate_failure_null_name() {
        var request = new SignupRequest(null, correctEmail);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("name");
                });
    }

    @Test
    @DisplayName("name이 공백이라 검증 실패")
    void validate_failure_blank_name() {
        var request = new SignupRequest("    ", correctEmail);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("name");
                });
    }

    @Test
    @DisplayName("name이 50자 초과하여 검증 실패")
    void validate_failure_long_name() {
        var request = new SignupRequest(longName, correctEmail);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("name");
                });
    }

    @Test
    @DisplayName("name이 정확히 50자면 통과")
    void validate_success_50_length_name() {
        var request = new SignupRequest(
                "A".repeat(50), correctEmail);

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("이모지는 하나 당 length 2씩 차지")
    void validate_failure_use_emoji() {
        var request = new SignupRequest(
                "😂".repeat(30), correctEmail
        );

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("name");
                });
    }

    @Test
    @DisplayName("email이 null이라 검증 실패")
    void validate_failure_null_email() {
        var request = new SignupRequest(correctName, null);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("email");
                });
    }

    @Test
    @DisplayName("email이 공백이라 NotBlank와 Email 위반")
    void validate_failure_blank_email() {
        var request = new SignupRequest(correctName, "    ");

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getConstraintDescriptor()
                                .getAnnotation()
                                .annotationType()
                                .getSimpleName()
                )
                .contains(
                        "NotBlank",
                        "Email"
                );
    }

    @Test
    @DisplayName("email이 형식에 맞지 않아 검증 실패")
    void validate_failure_not_email() {
        var request = new SignupRequest(correctName, "test");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("email");
                });
    }
}