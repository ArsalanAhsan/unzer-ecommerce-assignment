package com.unzer.assignment.ecommerce.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final RefundService refundService;

    public PaymentController(
            RefundService refundService
    ) {
        this.refundService = refundService;
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable UUID paymentId
    ) {

        RefundResponse response =
                refundService.refund(paymentId);

        return ResponseEntity.ok(response);
    }
}