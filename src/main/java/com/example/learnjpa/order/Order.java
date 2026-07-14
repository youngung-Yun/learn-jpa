package com.example.learnjpa.order;

import com.example.learnjpa.common.AuditingEntity;
import com.example.learnjpa.member.Member;
import com.example.learnjpa.order.exception.InvalidOrderStatusException;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@AttributeOverrides({
        @AttributeOverride(name = "createdAt",
                column = @Column(name = "ordered_at", nullable = false, updatable = false))
})
public class Order extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<OrderProduct> orderProductList = new ArrayList<>();

    protected Order() {
        status = OrderStatus.ORDERED;
    }

    public void cancel() {
        if (status == OrderStatus.CANCELED || status == OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException(status, OrderStatus.CANCELED);
        }
        status = OrderStatus.CANCELED;
    }

    public void deliver() {
        if (status == OrderStatus.CANCELED || status == OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException(status, OrderStatus.DELIVERED);
        }
        status = OrderStatus.DELIVERED;
    }
}

