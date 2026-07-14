package com.example.learnjpa.product;

import com.example.learnjpa.config.JpaConfig;
import com.example.learnjpa.product.dto.request.ProductRegisterRequest;
import com.example.learnjpa.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest(showSql = false)
@Import({JpaConfig.class, ProductService.class})
class ProductServiceIntegrationTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductService productService;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("제품 등록 성공")
    void product_register_success() {
        var request = new ProductRegisterRequest("product", 10_000L, 100L);

        Long id = productService.registerProduct(request);
        entityManager.clear();

        var found = productRepository.findById(id)
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getName()).isEqualTo(request.name());
        assertThat(found.getPrice()).isEqualTo(request.price());
        assertThat(found.getStockQuantity()).isEqualTo(request.stockQuantity());
    }
}