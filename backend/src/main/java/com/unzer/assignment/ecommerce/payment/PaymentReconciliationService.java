package com.unzer.assignment.ecommerce.payment;

import com.unzer.assignment.ecommerce.inventory.InventoryService;
import com.unzer.assignment.ecommerce.order.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private final OrderService orderService;
    private final InventoryService inventoryService;

    public PaymentReconciliationService(
            PaymentRepository paymentRepository,
            ProcessedWebhookRepository processedWebhookRepository,
            OrderService orderService,
            InventoryService inventoryService
    ) {
        this.paymentRepository = paymentRepository;
        this.processedWebhookRepository = processedWebhookRepository;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public void reconcile(
            PaymentWebhookRequest request,
            PaymentProviderStatus providerStatus
    ) {

        String eventKey =
                request.event()
                        + ":"
                        + request.paymentId()
                        + ":"
                        + providerStatus;

        if (processedWebhookRepository
                .existsByEventKey(eventKey)) {
            return;
        }

        Payment payment =
                paymentRepository
                        .findByProviderPaymentId(
                                request.paymentId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Unknown payment: "
                                                + request.paymentId()
                                )
                        );

        switch (providerStatus) {

            case SUCCEEDED -> {

                payment.markSucceeded();

                orderService.markPaid(
                        payment.getOrderId()
                );

                inventoryService.confirm(
                        payment.getOrderId()
                );
            }

            case FAILED -> {

                payment.markFailed();

                orderService.markPaymentFailed(
                        payment.getOrderId()
                );

                inventoryService.release(
                        payment.getOrderId()
                );
            }

            case PENDING -> {
                // Keep everything unchanged.
            }

            case REFUNDED -> {
                // Step 11.
            }
        }

        processedWebhookRepository.save(
                new ProcessedWebhook(eventKey)
        );
    }
}