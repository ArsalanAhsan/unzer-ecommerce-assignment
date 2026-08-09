package com.unzer.assignment.ecommerce.checkout;

import com.unzer.assignment.ecommerce.catalog.Product;
import com.unzer.assignment.ecommerce.catalog.ProductService;
import com.unzer.assignment.ecommerce.inventory.InventoryService;
import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.order.OrderService;
import org.springframework.stereotype.Service;
import com.unzer.assignment.ecommerce.payment.Payment;
import com.unzer.assignment.ecommerce.payment.PaymentService;
import com.unzer.assignment.ecommerce.payment.PaymentStartResult;

@Service
public class CheckoutService {

    private final ProductService productService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    public CheckoutService(
            ProductService productService,
            OrderService orderService,
            InventoryService inventoryService,
            PaymentService paymentService
    ) {
        this.productService = productService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
    }

    public CheckoutResponse checkout(CheckoutRequest request) {

        Product product =
                productService.getActiveProduct(
                        request.productId()
                );

        Order order =
                orderService.createOrder(
                        product,
                        request.quantity()
                );

        try {

            inventoryService.reserve(
                    order.getId(),
                    product.getId(),
                    request.quantity()
            );

        } catch (RuntimeException ex) {

            orderService.cancel(order.getId());

            throw ex;
        }

        orderService.markAwaitingPayment(
                order.getId()
        );

        Order updatedOrder =
                orderService.getOrder(
                        order.getId()
                );

        String idempotencyKey =
                "checkout-" + updatedOrder.getId();

        Payment payment =
                paymentService.createPayment(
                        updatedOrder,
                        request.paymentMethod(),
                        idempotencyKey
                );

        PaymentStartResult paymentResult =
                paymentService.startPayment(
                        payment,
                        updatedOrder,
                        request.paymentTypeId()
                );

        paymentService.attachProviderReferences(
                payment.getId(),
                paymentResult
        );

        return new CheckoutResponse(
                updatedOrder.getId(),
                updatedOrder.getStatus(),
                payment.getId(),
                updatedOrder.getTotalAmountMinor(),
                updatedOrder.getCurrency(),
                paymentResult.redirectUrl()
        );
    }
}