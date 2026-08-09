package com.unzer.assignment.ecommerce.payment;

import java.util.UUID;

public record RefundResponse(
        UUID paymentId,
        PaymentStatus status,
        String providerRefundId
) {
}