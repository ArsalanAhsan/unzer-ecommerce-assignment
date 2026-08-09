package com.unzer.assignment.ecommerce.payment;

public record PaymentWebhookRequest(
        String event,
        String publicKey,
        String retrieveUrl,
        String paymentId
) {
}