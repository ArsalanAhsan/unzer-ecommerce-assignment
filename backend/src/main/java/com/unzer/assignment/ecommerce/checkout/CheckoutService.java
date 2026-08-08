package com.unzer.assignment.ecommerce.checkout;

import com.unzer.assignment.ecommerce.catalog.Product;
import com.unzer.assignment.ecommerce.catalog.ProductService;
import com.unzer.assignment.ecommerce.inventory.InventoryService;
import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.order.OrderService;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final ProductService productService;
    private final OrderService orderService;
    private final InventoryService inventoryService;

    public CheckoutService(
            ProductService productService,
            OrderService orderService,
            InventoryService inventoryService
    ) {
        this.productService = productService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
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

        return new CheckoutResponse(
                updatedOrder.getId(),
                updatedOrder.getStatus(),
                updatedOrder.getTotalAmountMinor(),
                updatedOrder.getCurrency()
        );
    }
}