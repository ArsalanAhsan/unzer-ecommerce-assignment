package com.unzer.assignment.ecommerce.order;

public enum OrderStatus {

    CREATED,
    AWAITING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,

    // Modelled for the complete lifecycle,
    // even though the vertical slice does not implement fulfillment.
    FULFILLING,
    SHIPPED,
    COMPLETED,
    REFUNDED
}