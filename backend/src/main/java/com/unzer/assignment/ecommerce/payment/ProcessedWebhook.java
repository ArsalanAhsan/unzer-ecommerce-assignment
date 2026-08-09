package com.unzer.assignment.ecommerce.payment;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "processed_webhook")
public class ProcessedWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, unique = true)
    private String eventKey;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedWebhook() {
    }

    public ProcessedWebhook(String eventKey) {
        this.eventKey = eventKey;
        this.processedAt = Instant.now();
    }
}