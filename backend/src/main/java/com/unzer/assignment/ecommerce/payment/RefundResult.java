package com.unzer.assignment.ecommerce.payment;

public record RefundResult(
        String refundTransactionId,
        boolean succeeded
) {
}