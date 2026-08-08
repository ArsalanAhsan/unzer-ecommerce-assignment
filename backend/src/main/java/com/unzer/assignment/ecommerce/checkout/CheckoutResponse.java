package com.unzer.assignment.ecommerce.checkout;

import com.unzer.assignment.ecommerce.order.OrderStatus;

import java.util.UUID;

public record CheckoutResponse(
        UUID orderId,
        OrderStatus status,
        Long totalAmountMinor,
        String currency
) {
}