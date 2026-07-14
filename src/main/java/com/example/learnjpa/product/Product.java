package com.example.learnjpa.product;

import com.example.learnjpa.common.AuditingEntity;
import com.example.learnjpa.order.OrderProduct;
import com.example.learnjpa.product.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Product extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(name = "stock_quantity", nullable = false)
    private Long stockQuantity;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<OrderProduct> orderProductList = new ArrayList<>();

    @Builder
    public Product(String name, Long price, Long stockQuantity) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public void increaseStockQuantity(long increase) {
        this.stockQuantity += increase;
    }

    public void decreaseStockQuantity(long decrease) {
        if (this.stockQuantity < decrease) {
            throw new InsufficientStockException();
        }
        stockQuantity -= decrease;
    }

    public void addOrderProduct(OrderProduct orderProduct) {
        orderProductList.add(orderProduct);
    }
}
