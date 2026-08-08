package com.unzer.assignment.ecommerce.inventory;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException(Long productId) {
        super("Insufficient stock for product: " + productId);
    }
}