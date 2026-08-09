package com.unzer.assignment.ecommerce.order;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount_minor", nullable = false)
    private Long totalAmountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // Required by JPA
    }

    public Order(Long totalAmountMinor, String currency) {
        this.id = UUID.randomUUID();
        this.status = OrderStatus.CREATED;
        this.totalAmountMinor = totalAmountMinor;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.assignTo(this);
    }

    public void markAwaitingPayment() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Order must be CREATED before awaiting payment"
            );
        }

        status = OrderStatus.AWAITING_PAYMENT;
    }

    public void markPaid() {
        if (status == OrderStatus.PAID) {
            return; // idempotent
        }

        if (status != OrderStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException(
                    "Only an order awaiting payment can be marked as paid"
            );
        }

        status = OrderStatus.PAID;
    }

    public void markPaymentFailed() {
        if (status == OrderStatus.PAYMENT_FAILED) {
            return; // idempotent
        }

        if (status != OrderStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException(
                    "Only an order awaiting payment can fail payment"
            );
        }

        status = OrderStatus.PAYMENT_FAILED;
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            return;
        }

        if (status == OrderStatus.PAID) {
            throw new IllegalStateException(
                    "Paid order cannot be directly cancelled"
            );
        }

        status = OrderStatus.CANCELLED;
    }

    public void markRefunded() {

        if (status == OrderStatus.REFUNDED) {
            return;
        }

        if (status != OrderStatus.PAID) {
            throw new IllegalStateException(
                    "Only a paid order can be refunded"
            );
        }

        status = OrderStatus.REFUNDED;
    }

    public UUID getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Long getTotalAmountMinor() {
        return totalAmountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}