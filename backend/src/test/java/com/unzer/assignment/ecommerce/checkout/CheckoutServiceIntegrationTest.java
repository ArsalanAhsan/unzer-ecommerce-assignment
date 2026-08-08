package com.unzer.assignment.ecommerce.checkout;

import com.unzer.assignment.ecommerce.inventory.Inventory;
import com.unzer.assignment.ecommerce.inventory.InventoryRepository;
import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.order.OrderRepository;
import com.unzer.assignment.ecommerce.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CheckoutServiceIntegrationTest {

    @BeforeEach
    void resetInventory() {
        inventoryRepository.resetStock(
                1L,
                10,
                0
        );
    }

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldCreateOrderAndReserveInventory() {

        // Given
        CheckoutRequest request =
                new CheckoutRequest(
                        1L,
                        2
                );

        // When
        CheckoutResponse response =
                checkoutService.checkout(request);

        // Then
        Order order =
                orderRepository.findById(
                        response.orderId()
                ).orElseThrow();

        assertThat(order.getStatus())
                .isEqualTo(
                        OrderStatus.AWAITING_PAYMENT
                );

        assertThat(order.getTotalAmountMinor())
                .isEqualTo(5998L);

        Inventory inventory =
                inventoryRepository.findByProductId(1L)
                        .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(8);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(2);
    }
}