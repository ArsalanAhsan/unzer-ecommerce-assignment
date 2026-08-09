package com.unzer.assignment.ecommerce.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookRepository
        extends JpaRepository<ProcessedWebhook, Long> {

    boolean existsByEventKey(String eventKey);
}