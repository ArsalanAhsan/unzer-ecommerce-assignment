package com.unzer.assignment.ecommerce.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public void reserve(
            UUID orderId,
            Long productId,
            int quantity
    ) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        int affectedRows =
                inventoryRepository.reserve(productId, quantity);

        if (affectedRows == 0) {
            throw new OutOfStockException(productId);
        }

        InventoryReservation reservation =
                new InventoryReservation(
                        orderId,
                        productId,
                        quantity,
                        Instant.now().plus(15, ChronoUnit.MINUTES)
                );

        reservationRepository.save(reservation);
    }

    @Transactional
    public void confirm(UUID orderId) {

        List<InventoryReservation> reservations =
                reservationRepository.findByOrderIdAndStatus(
                        orderId,
                        ReservationStatus.ACTIVE
                );

        for (InventoryReservation reservation : reservations) {

            inventoryRepository.confirm(
                    reservation.getProductId(),
                    reservation.getQuantity()
            );

            reservation.confirm();
        }
    }

    @Transactional
    public void release(UUID orderId) {

        List<InventoryReservation> reservations =
                reservationRepository.findByOrderIdAndStatus(
                        orderId,
                        ReservationStatus.ACTIVE
                );

        for (InventoryReservation reservation : reservations) {

            inventoryRepository.release(
                    reservation.getProductId(),
                    reservation.getQuantity()
            );

            reservation.release();
        }
    }
}