package com.example.learnjpa.product;

import com.example.learnjpa.product.dto.request.ProductIncreaseQuantityRequest;
import com.example.learnjpa.product.dto.request.ProductRegisterRequest;
import com.example.learnjpa.product.dto.request.ProductRequest;
import com.example.learnjpa.product.dto.response.ProductResponse;
import com.example.learnjpa.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse getById(ProductRequest request) {
        var id = request.id();

        var product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 Product id입니다: %d".formatted(id)));

        return toResponse(product);
    }

    @Transactional
    public Long registerProduct(ProductRegisterRequest request) {
        return productRepository.save(toEntity(request))
                .getId();
    }

    @Transactional
    public void increaseQuantity(ProductIncreaseQuantityRequest request) {
        var productId = request.productId();
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 Product id입니다: %d".formatted(productId)));

        product.increaseStockQuantity(request.increaseQuantity());
    }

    private Product toEntity(ProductRegisterRequest request) {
        return Product.builder()
                .name(request.name())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .build();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .quantity(product.getStockQuantity())
                .build();
    }
}
