package com.unzer.assignment.ecommerce.payment;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void shouldStartAsPending() {

        Payment payment =
                createPayment();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldMarkPendingPaymentAsSucceeded() {

        Payment payment =
                createPayment();

        payment.markSucceeded();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void shouldMarkPendingPaymentAsFailed() {

        Payment payment =
                createPayment();

        payment.markFailed();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldRefundSuccessfulPayment() {

        Payment payment =
                createPayment();

        payment.markSucceeded();

        payment.markRefundPending();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.REFUND_PENDING);

        payment.markRefunded();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void shouldRejectRefundForPendingPayment() {

        Payment payment =
                createPayment();

        assertThatThrownBy(
                payment::markRefundPending
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "Only a successful payment can be refunded"
                );

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    private Payment createPayment() {

        return new Payment(
                UUID.randomUUID(),
                PaymentMethod.CREDIT_CARD,
                2999L,
                "EUR",
                "test-idempotency-"
                        + UUID.randomUUID()
        );
    }
}