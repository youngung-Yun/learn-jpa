package com.example.learnjpa.product.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductRegisterRequestTest {

    private static final String name = "product";

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
    @DisplayName("price가 음수라 실패")
    void validate_failure_price_0() {
        var request = new ProductRegisterRequest(name, -10L, 100L);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("price");
                });
    }

    @Test
    @DisplayName("stockQuantity가 음수라 실패")
    void validate_failure_stockQuantity_0() {
        var request = new ProductRegisterRequest(name, 10_000L, -10L);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("stockQuantity");
                });
    }
}