package com.example.learnjpa.product;

import com.example.learnjpa.product.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("Product의 재고 증가 성공")
    void increase_stock_quantity_success() {
        long defaultStockQuantity = 5L;

        Product product = Product.builder()
                .name("name")
                .price(10_000L)
                .stockQuantity(defaultStockQuantity)
                .build();

        product.increaseStockQuantity(5L);

        assertThat(product.getStockQuantity()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Product의 재고 감소 성공")
    void decrease_stock_quantity_success() {

        long defaultStockQuantity = 10L;

        Product product = Product
                .builder()
                .name("name")
                .price(10_000L)
                .stockQuantity(defaultStockQuantity)
                .build();

        product.decreaseStockQuantity(5L);

        assertThat(product.getStockQuantity()).isEqualTo(5L);
    }

    @Test
    @DisplayName("현재 재고보다 많은 재고 감소로 예외 발생")
    void decrease_stock_quantity_throw() {

        long defaultStockQuantity = 5L;

        Product product = Product.builder()
                .name("name")
                .price(10_000L)
                .stockQuantity(defaultStockQuantity)
                .build();

        assertThatThrownBy(() -> product.decreaseStockQuantity(100L))
                .isInstanceOf(InsufficientStockException.class);
    }
}