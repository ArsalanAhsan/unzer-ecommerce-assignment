package com.unzer.assignment.ecommerce.order;

import jakarta.persistence.*;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private String sku;

    @Column(name = "unit_price_minor", nullable = false)
    private Long unitPriceMinor;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {
        // Required by JPA
    }

    public OrderItem(
            Long productId,
            String productName,
            String sku,
            Long unitPriceMinor,
            int quantity
    ) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.unitPriceMinor = unitPriceMinor;
        this.quantity = quantity;
    }

    void assignTo(Order order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSku() {
        return sku;
    }

    public Long getUnitPriceMinor() {
        return unitPriceMinor;
    }

    public int getQuantity() {
        return quantity;
    }
}