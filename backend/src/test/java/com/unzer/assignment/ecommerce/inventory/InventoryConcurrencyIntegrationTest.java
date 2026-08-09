package com.unzer.assignment.ecommerce.inventory;

import com.unzer.assignment.ecommerce.catalog.Product;
import com.unzer.assignment.ecommerce.catalog.ProductRepository;
import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.order.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InventoryConcurrencyIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {

        transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );

        /*
         * Only one item is available.
         */
        transactionTemplate.executeWithoutResult(
                status ->
                        inventoryRepository.resetStock(
                                1L,
                                1,
                                0
                        )
        );
    }

    @AfterEach
    void resetInventory() {

        transactionTemplate.executeWithoutResult(
                status ->
                        inventoryRepository.resetStock(
                                1L,
                                10,
                                0
                        )
        );
    }

    @Test
    void shouldAllowOnlyOneReservationForLastUnitOfStock()
            throws Exception {

        // Given
        Product product =
                productRepository.findByIdAndActiveTrue(1L)
                        .orElseThrow();

        Order firstOrder =
                orderService.createOrder(
                        product,
                        1
                );

        Order secondOrder =
                orderService.createOrder(
                        product,
                        1
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch readyLatch =
                new CountDownLatch(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        try {

            Future<Boolean> firstResult =
                    executor.submit(() -> {

                        readyLatch.countDown();
                        startLatch.await();

                        try {

                            inventoryService.reserve(
                                    firstOrder.getId(),
                                    product.getId(),
                                    1
                            );

                            return true;

                        } catch (OutOfStockException exception) {

                            return false;
                        }
                    });

            Future<Boolean> secondResult =
                    executor.submit(() -> {

                        readyLatch.countDown();
                        startLatch.await();

                        try {

                            inventoryService.reserve(
                                    secondOrder.getId(),
                                    product.getId(),
                                    1
                            );

                            return true;

                        } catch (OutOfStockException exception) {

                            return false;
                        }
                    });

            /*
             * Wait until both threads are ready.
             */
            readyLatch.await();

            /*
             * Release both threads at almost exactly
             * the same time.
             */
            startLatch.countDown();

            boolean firstSucceeded =
                    firstResult.get();

            boolean secondSucceeded =
                    secondResult.get();

            // Then
            int successfulReservations =
                    (firstSucceeded ? 1 : 0)
                            + (secondSucceeded ? 1 : 0);

            assertThat(successfulReservations)
                    .isEqualTo(1);

            Inventory inventory =
                    inventoryRepository
                            .findByProductId(1L)
                            .orElseThrow();

            assertThat(inventory.getAvailableQuantity())
                    .isEqualTo(0);

            assertThat(inventory.getReservedQuantity())
                    .isEqualTo(1);

            int firstOrderReservations =
                    reservationRepository
                            .findByOrderIdAndStatus(
                                    firstOrder.getId(),
                                    ReservationStatus.ACTIVE
                            )
                            .size();

            int secondOrderReservations =
                    reservationRepository
                            .findByOrderIdAndStatus(
                                    secondOrder.getId(),
                                    ReservationStatus.ACTIVE
                            )
                            .size();

            assertThat(
                    firstOrderReservations
                            + secondOrderReservations
            ).isEqualTo(1);

        } finally {

            executor.shutdownNow();
        }
    }
}