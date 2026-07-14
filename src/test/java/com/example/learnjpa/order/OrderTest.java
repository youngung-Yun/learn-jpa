package com.example.learnjpa.order;

import com.example.learnjpa.member.Member;
import com.example.learnjpa.order.exception.InvalidOrderStatusException;
import com.example.learnjpa.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    @DisplayName("주문 상태를 'CANCELED'로 변경")
    void change_status_to_canceled() {
        Order order = new Order();
        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("주문 상태를 'DELIVERED'로 변경")
    void change_status_to_delivered() {
        Order order = new Order();
        order.deliver();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("'CANCELED'인 상태에서 다른 상태로 변경 불가능")
    void change_status_from_canceled_throw() {
        Order order = new Order();
        order.cancel();

        assertThatThrownBy(() -> order.cancel())
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(() -> order.deliver())
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("'DELIVERED'인 상태에서 다른 상태로 변경 불가능")
    void change_status_from_delivered_throw() {
        Order order = new Order();
        order.deliver();

        assertThatThrownBy(() -> order.cancel())
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(() -> order.deliver())
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("Order 생성자로 생성 시 Member와 양방향 연관관계가 생성되는지 확인")
    void order_create_bidirectional_relation() {
        var member = Member.builder()
                .name("name")
                .email("test@example.com")
                .build();

        var order = new Order(member);

        assertThat(order.getMember()).isSameAs(member);
        assertThat(member.getOrderList()).containsExactly(order);
    }

    @Test
    @DisplayName("OrderProduct 생성자로 생성 시 Order와 양방향 연관관계가 생성되는지 확인")
    void orderProduct_related_to_order() {
        var order = createOrder();
        var product = createProduct();
        var orderProduct = OrderProduct.builder()
                .order(order)
                .product(product)
                .orderPrice(10_000L)
                .quantity(100L)
                .build();

        assertThat(order.getOrderProductList()).containsExactly(orderProduct);
        assertThat(orderProduct.getOrder()).isSameAs(order);
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