package com.unzer.assignment.ecommerce.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void shouldStartAsCreated() {

        Order order =
                createOrder();

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void shouldMoveFromCreatedToAwaitingPayment() {

        Order order =
                createOrder();

        order.markAwaitingPayment();

        assertThat(order.getStatus())
                .isEqualTo(
                        OrderStatus.AWAITING_PAYMENT
                );
    }

    @Test
    void shouldMarkAwaitingPaymentOrderAsPaid() {

        Order order =
                createOrder();

        order.markAwaitingPayment();

        order.markPaid();

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.PAID);
    }

    @Test
    void shouldRejectInvalidPaidTransition() {

        Order order =
                createOrder();

        assertThatThrownBy(
                order::markPaid
        )
                .isInstanceOf(
                        IllegalStateException.class
                );

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void shouldRefundPaidOrder() {

        Order order =
                createOrder();

        order.markAwaitingPayment();
        order.markPaid();
        order.markRefunded();

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.REFUNDED);
    }

    private Order createOrder() {

        return new Order(
                2999L,
                "EUR"
        );
    }
}