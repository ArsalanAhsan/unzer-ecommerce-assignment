package com.unzer.assignment.ecommerce.payment;

import com.unzer.assignment.ecommerce.order.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentProvider paymentProvider
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
    }

    @Transactional
    public Payment createPayment(
            Order order,
            PaymentMethod method,
            String idempotencyKey
    ) {

        return paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseGet(() ->
                        paymentRepository.save(
                                new Payment(
                                        order.getId(),
                                        method,
                                        order.getTotalAmountMinor(),
                                        order.getCurrency(),
                                        idempotencyKey
                                )
                        )
                );
    }

    public PaymentStartResult startPayment(
            Payment payment,
            Order order,
            String paymentTypeId
    ) {

        // External Unzer network call.
        // Intentionally NOT inside a database transaction.
        return paymentProvider.startPayment(
                order,
                payment.getMethod(),
                paymentTypeId
        );
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId) {

        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId)
                );
    }

    public RefundResult refundWithProvider(
            Payment payment
    ) {

        return paymentProvider.refund(
                payment.getProviderPaymentId(),
                payment.getProviderTransactionId()
        );
    }

    @Transactional
    public void markRefunded(UUID paymentId) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId)
                );

        payment.markRefunded();
    }

    @Transactional
    public void markRefundPending(UUID paymentId) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId)
                );

        payment.markRefundPending();
    }

    @Transactional
    public void attachProviderReferences(
            UUID paymentId,
            PaymentStartResult result
    ) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow();

        payment.attachProviderReferences(
                result.paymentTypeId(),
                result.paymentId(),
                result.transactionId()
        );
    }
}