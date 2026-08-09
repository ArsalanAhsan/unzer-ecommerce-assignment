package com.unzer.assignment.ecommerce.payment;

import com.unzer.assignment.ecommerce.order.OrderService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RefundService {

    private final PaymentService paymentService;
    private final OrderService orderService;

    public RefundService(
            PaymentService paymentService,
            OrderService orderService
    ) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    public RefundResponse refund(UUID paymentId) {

        Payment payment =
                paymentService.getPayment(paymentId);

        if (payment.getStatus() == PaymentStatus.REFUNDED) {

            return new RefundResponse(
                    payment.getId(),
                    payment.getStatus(),
                    null
            );
        }

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException(
                    "Only successful payments can be refunded"
            );
        }

        /*
         * Persist intent before external network call.
         */
        paymentService.markRefundPending(paymentId);

        /*
         * External Unzer call.
         * No database transaction is held here.
         */
        RefundResult result =
                paymentService.refundWithProvider(payment);

        /*
         * If Unzer already confirms success synchronously,
         * update our local state immediately.
         *
         * Webhook reconciliation remains idempotent and can
         * confirm the same state later.
         */
        if (result.succeeded()) {

            paymentService.markRefunded(paymentId);

            orderService.markRefunded(
                    payment.getOrderId()
            );
        }

        Payment updatedPayment =
                paymentService.getPayment(paymentId);

        return new RefundResponse(
                updatedPayment.getId(),
                updatedPayment.getStatus(),
                result.refundTransactionId()
        );
    }
}