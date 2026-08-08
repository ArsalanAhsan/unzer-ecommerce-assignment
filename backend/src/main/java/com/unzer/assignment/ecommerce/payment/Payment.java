package com.unzer.assignment.ecommerce.payment;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "provider_payment_type_id")
    private String providerPaymentTypeId;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
        // Required by JPA
    }

    public Payment(
            UUID orderId,
            PaymentMethod method,
            Long amountMinor,
            String currency,
            String idempotencyKey
    ) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.method = method;
        this.status = PaymentStatus.PENDING;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void attachProviderReferences(
            String paymentTypeId,
            String transactionId
    ) {
        this.providerPaymentTypeId = paymentTypeId;
        this.providerTransactionId = transactionId;
        this.updatedAt = Instant.now();
    }

    public void markSucceeded() {

        if (status == PaymentStatus.SUCCEEDED) {
            return;
        }

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a pending payment can succeed"
            );
        }

        status = PaymentStatus.SUCCEEDED;
        updatedAt = Instant.now();
    }

    public void markFailed() {

        if (status == PaymentStatus.FAILED) {
            return;
        }

        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a pending payment can fail"
            );
        }

        status = PaymentStatus.FAILED;
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getProviderPaymentTypeId() {
        return providerPaymentTypeId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }
}