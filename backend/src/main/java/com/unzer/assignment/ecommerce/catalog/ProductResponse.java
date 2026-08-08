package com.unzer.assignment.ecommerce.catalog;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        Long priceMinor,
        String currency
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPriceMinor(),
                product.getCurrency()
        );
    }
}