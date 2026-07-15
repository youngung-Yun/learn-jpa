package com.example.learnjpa.order.dto.request;

import com.example.learnjpa.product.dto.request.ProductRegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCreateRequestTest {

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
    @DisplayName("memberId가 0이라 실패")
    void validate_failure_memberId_0() {
        var list = List.of(
                new OrderCreateRequest.OrderCreateProductRequest(
                        1L, 100L
                )
        );

        var request = new OrderCreateRequest(0L, list);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("memberId");
                });
    }

    @Test
    @DisplayName("memberId가 음수라 실패")
    void validate_failure_memberId_negative() {
        var list = List.of(
                new OrderCreateRequest.OrderCreateProductRequest(
                        10L, 100L
                )
        );

        var request = new OrderCreateRequest(-10L, list);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("memberId");
                });
    }

    @Test
    @DisplayName("OrderCreateProductRequest가 비어있어 실패")
    void validate_failure_list_empty() {
        List<OrderCreateRequest.OrderCreateProductRequest> list = List.of();

        var request = new OrderCreateRequest(10L, list);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anySatisfy((violation) -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("createOrderProductList");
                });
    }
}