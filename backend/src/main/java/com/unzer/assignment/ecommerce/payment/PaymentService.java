package com.unzer.assignment.ecommerce.payment;

import com.unzer.assignment.ecommerce.order.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unzer.assignment.ecommerce.payment.PaymentRepository;


@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(
            PaymentRepository paymentRepository
    ) {
        this.paymentRepository = paymentRepository;
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
}