package com.unzer.assignment.ecommerce.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.unzer.assignment.ecommerce.catalog.Product;
import com.unzer.assignment.ecommerce.catalog.ProductRepository;
import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.order.OrderService;
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

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldReserveInventory() {

        // Given
        Product product = productRepository
                .findByIdAndActiveTrue(1L)
                .orElseThrow();

        Order order = orderService.createOrder(
                product,
                2
        );

        // When
        inventoryService.reserve(
                order.getId(),
                product.getId(),
                2
        );

        // Then
        Inventory inventory = inventoryRepository
                .findByProductId(product.getId())
                .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(8);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(2);

        assertThat(
                reservationRepository.findByOrderIdAndStatus(
                        order.getId(),
                        ReservationStatus.ACTIVE
                )
        ).hasSize(1);
    }

    @Test
    void shouldRejectReservationWhenStockIsInsufficient() {

        // Given
        Product product = productRepository
                .findByIdAndActiveTrue(1L)
                .orElseThrow();

        Order order = orderService.createOrder(
                product,
                1
        );

        // When + Then
        assertThatThrownBy(() ->
                inventoryService.reserve(
                        order.getId(),
                        product.getId(),
                        100
                )
        )
                .isInstanceOf(OutOfStockException.class);

        Inventory inventory = inventoryRepository
                .findByProductId(product.getId())
                .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(10);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(0);
    }
}