package com.unzer.assignment.ecommerce.payment;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/unzer")
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    public PaymentWebhookController(
            PaymentWebhookService webhookService
    ) {
        this.webhookService = webhookService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> webhook(
            @RequestBody PaymentWebhookRequest request
    ) {

        webhookService.process(request);

        return ResponseEntity.ok().build();
    }
}