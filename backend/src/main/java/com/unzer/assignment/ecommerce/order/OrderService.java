package com.unzer.assignment.ecommerce.order;

import com.unzer.assignment.ecommerce.catalog.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(
            Product product,
            int quantity
    ) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        long totalAmountMinor =
                Math.multiplyExact(
                        product.getPriceMinor(),
                        quantity
                );

        Order order = new Order(
                totalAmountMinor,
                product.getCurrency()
        );

        OrderItem item = new OrderItem(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPriceMinor(),
                quantity
        );

        order.addItem(item);

        return orderRepository.save(order);
    }

    @Transactional
    public void markAwaitingPayment(UUID orderId) {
        getRequiredOrder(orderId)
                .markAwaitingPayment();
    }

    @Transactional
    public void markPaid(UUID orderId) {
        getRequiredOrder(orderId)
                .markPaid();
    }

    @Transactional
    public void markPaymentFailed(UUID orderId) {
        getRequiredOrder(orderId)
                .markPaymentFailed();
    }

    @Transactional
    public void cancel(UUID orderId) {
        getRequiredOrder(orderId)
                .cancel();
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        return getRequiredOrder(orderId);
    }

    private Order getRequiredOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(orderId)
                );
    }
}