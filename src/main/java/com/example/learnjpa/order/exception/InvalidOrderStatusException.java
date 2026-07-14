package com.example.learnjpa.order.exception;

import com.example.learnjpa.order.OrderStatus;

public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(OrderStatus current, OrderStatus next) {
        super("상태를 변경할 수 없습니다. current=%s, next=%s"
                .formatted(current, next));
    }
}
