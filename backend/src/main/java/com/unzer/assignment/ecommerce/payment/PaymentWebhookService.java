package com.unzer.assignment.ecommerce.payment;

import com.unzer.assignment.ecommerce.inventory.InventoryService;
import com.unzer.assignment.ecommerce.order.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookService {

    private final PaymentProvider paymentProvider;
    private final PaymentReconciliationService reconciliationService;

    public PaymentWebhookService(
            PaymentProvider paymentProvider,
            PaymentReconciliationService reconciliationService
    ) {
        this.paymentProvider = paymentProvider;
        this.reconciliationService = reconciliationService;
    }

    public void process(
            PaymentWebhookRequest request
    ) {

        if (request.paymentId() == null
                || request.paymentId().isBlank()) {
            return;
        }

        PaymentProviderStatus providerStatus =
                paymentProvider.getPaymentStatus(
                        request.paymentId()
                );

        reconciliationService.reconcile(
                request,
                providerStatus
        );
    }
}