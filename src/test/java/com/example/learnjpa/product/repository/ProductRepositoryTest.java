package com.example.learnjpa.product.repository;

import com.example.learnjpa.config.JpaConfig;
import com.example.learnjpa.product.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("Product 매핑 테스트")
    void save_test() {
        var name = "product1";
        var price = 10_000L;
        var quantity = 100L;


        Product product = Product.builder()
                .name(name)
                .price(price)
                .stockQuantity(quantity)
                .build();

        var saved = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        var found = productRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(found.getName()).isEqualTo(saved.getName());
        assertThat(found.getPrice()).isEqualTo(saved.getPrice());
        assertThat(found.getStockQuantity()).isEqualTo(saved.getStockQuantity());
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }
}