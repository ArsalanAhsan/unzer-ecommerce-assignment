package com.unzer.assignment.ecommerce.payment;

public record PaymentStartResult(
        String paymentTypeId,
        String transactionId,
        String redirectUrl
) {
}