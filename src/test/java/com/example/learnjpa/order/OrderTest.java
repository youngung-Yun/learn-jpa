package com.example.learnjpa.order;

import com.example.learnjpa.order.exception.InvalidOrderStatusException;
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
}