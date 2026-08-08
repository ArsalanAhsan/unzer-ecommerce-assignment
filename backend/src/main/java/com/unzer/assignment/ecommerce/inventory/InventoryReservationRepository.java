package com.unzer.assignment.ecommerce.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findByOrderIdAndStatus(
            UUID orderId,
            ReservationStatus status
    );
}