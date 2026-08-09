package com.unzer.assignment.ecommerce.payment;

public record PaymentStartResult(
        String paymentTypeId,
        String paymentId,
        String transactionId,
        String redirectUrl
) {
}