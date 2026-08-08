package com.unzer.assignment.ecommerce.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(name = "price_minor", nullable = false)
    private Long priceMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    protected Product() {
        // Required by JPA
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public Long getPriceMinor() {
        return priceMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }
}