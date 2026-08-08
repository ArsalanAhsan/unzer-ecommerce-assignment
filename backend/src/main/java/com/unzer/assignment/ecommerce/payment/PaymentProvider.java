package com.unzer.assignment.ecommerce.payment;

import com.unzer.assignment.ecommerce.order.Order;

public interface PaymentProvider {

    PaymentStartResult startPayment(
            Order order,
            PaymentMethod method,
            String paymentTypeId
    );
}