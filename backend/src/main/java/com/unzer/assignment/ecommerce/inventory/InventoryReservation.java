package com.unzer.assignment.ecommerce.inventory;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservation")
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected InventoryReservation() {
    }

    public InventoryReservation(
            UUID orderId,
            Long productId,
            int quantity,
            Instant expiresAt
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = ReservationStatus.ACTIVE;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void release() {
        this.status = ReservationStatus.RELEASED;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }
}