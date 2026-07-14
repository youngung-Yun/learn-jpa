package com.example.learnjpa.product;

import com.example.learnjpa.member.Member;
import com.example.learnjpa.order.Order;
import com.example.learnjpa.order.OrderProduct;
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
    @DisplayName("음수의 재고 증가 시도하여 예외 발생")
    void increase_negative_quantity_throw() {
        long quantity = -10L;

        Product product = Product.builder()
                .name("name")
                .price(10_000L)
                .stockQuantity(100L)
                .build();

        assertThatThrownBy(() -> product.increaseStockQuantity(quantity))
                .isInstanceOf(IllegalArgumentException.class);
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

    @Test
    @DisplayName("음수의 재고 감소 시도하여 예외 발생")
    void decrease_negative_quantity_throw() {
        long quantity = -10L;

        Product product = Product.builder()
                .name("name")
                .price(10_000L)
                .stockQuantity(100L)
                .build();

        assertThatThrownBy(() -> product.decreaseStockQuantity(quantity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("현재 재고와 같은 양의 재고 감소 엣지 케이스")
    void decrease_same_quantity() {
        long quantity = 5L;

        Product product = Product.builder()
                .name("name")
                .price(10_000L)
                .stockQuantity(quantity)
                .build();

        product.decreaseStockQuantity(quantity);

        assertThat(product.getStockQuantity()).isEqualTo(0L);
    }

    @Test
    @DisplayName("OrderProduct 생성 시 Product와 양방향 연관관계가 생성되는지 테스트")
    void orderProduct_related_to_product() {
        var order = createOrder();
        var product = createProduct();

        var orderProduct = OrderProduct.builder()
                .order(order)
                .product(product)
                .orderPrice(10_000L)
                .quantity(100L)
                .build();

        assertThat(product.getOrderProductList()).containsExactly(orderProduct);
        assertThat(orderProduct.getProduct()).isSameAs(product);
    }

    private Order createOrder() {
        var member = Member.builder()
                .name("name")
                .email("test@example.com")
                .build();

        return new Order(member);
    }

    private Product createProduct() {
        return Product.builder()
                .name("product1")
                .price(10_000L)
                .stockQuantity(100L)
                .build();
    }
}