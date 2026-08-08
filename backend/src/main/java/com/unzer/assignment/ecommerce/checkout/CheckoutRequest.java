package com.unzer.assignment.ecommerce.checkout;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(

        @NotNull
        Long productId,

        @Min(1)
        int quantity
) {
}