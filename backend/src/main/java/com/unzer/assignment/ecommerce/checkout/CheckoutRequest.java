package com.unzer.assignment.ecommerce.checkout;

import com.unzer.assignment.ecommerce.payment.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(

        @NotNull
        Long productId,

        @Min(1)
        int quantity,

        @NotNull
        PaymentMethod paymentMethod,

        String paymentTypeId
) {
}