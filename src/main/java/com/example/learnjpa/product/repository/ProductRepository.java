package com.example.learnjpa.product.repository;

import com.example.learnjpa.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Product save(Product product);
}
