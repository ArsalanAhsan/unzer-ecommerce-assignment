package com.unzer.assignment.ecommerce.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryServiceIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void shouldReserveInventory() {

        // Given
        UUID orderId = UUID.randomUUID();

        Inventory before = inventoryRepository
                .findByProductId(1L)
                .orElseThrow();

        assertThat(before.getAvailableQuantity()).isEqualTo(10);
        assertThat(before.getReservedQuantity()).isEqualTo(0);

        // When
        inventoryService.reserve(orderId, 1L, 2);

        // Then
        Inventory after = inventoryRepository
                .findByProductId(1L)
                .orElseThrow();

        assertThat(after.getAvailableQuantity()).isEqualTo(8);
        assertThat(after.getReservedQuantity()).isEqualTo(2);

        assertThat(
                reservationRepository.findByOrderIdAndStatus(
                        orderId,
                        ReservationStatus.ACTIVE
                )
        ).hasSize(1);
    }

    @Test
    void shouldRejectReservationWhenStockIsInsufficient() {

        // Given
        UUID orderId = UUID.randomUUID();

        // When + Then
        assertThatThrownBy(() ->
                inventoryService.reserve(orderId, 1L, 100)
        )
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("1");

        Inventory inventory = inventoryRepository
                .findByProductId(1L)
                .orElseThrow();

        assertThat(inventory.getAvailableQuantity()).isEqualTo(10);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
    }
}