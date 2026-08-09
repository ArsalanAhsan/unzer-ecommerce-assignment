package com.unzer.assignment.ecommerce.checkout;

import com.unzer.assignment.ecommerce.order.OrderStatus;

import java.util.UUID;

public record CheckoutResponse(
        UUID orderId,
        OrderStatus status,
        UUID paymentId,
        Long totalAmountMinor,
        String currency,
        String redirectUrl
) {
}