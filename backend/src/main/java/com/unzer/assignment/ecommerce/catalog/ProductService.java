package com.unzer.assignment.ecommerce.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getProducts() {
        return productRepository.findAllByActiveTrue()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
    @Transactional(readOnly = true)
    public Product getActiveProduct(Long productId) {
        return productRepository
                .findByIdAndActiveTrue(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(productId)
                );
    }
}