package com.unzer.assignment.ecommerce.payment;

import com.unzer.assignment.ecommerce.catalog.Product;
import com.unzer.assignment.ecommerce.catalog.ProductRepository;
import com.unzer.assignment.ecommerce.inventory.Inventory;
import com.unzer.assignment.ecommerce.inventory.InventoryRepository;
import com.unzer.assignment.ecommerce.inventory.InventoryReservation;
import com.unzer.assignment.ecommerce.inventory.InventoryReservationRepository;
import com.unzer.assignment.ecommerce.inventory.InventoryService;
import com.unzer.assignment.ecommerce.inventory.ReservationStatus;
import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.order.OrderRepository;
import com.unzer.assignment.ecommerce.order.OrderService;
import com.unzer.assignment.ecommerce.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaymentReconciliationServiceIntegrationTest {

    @Autowired
    private PaymentReconciliationService reconciliationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessedWebhookRepository processedWebhookRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @BeforeEach
    void resetInventory() {
        inventoryRepository.resetStock(
                1L,
                10,
                0
        );
    }

    @Test
    void shouldConfirmOrderAndInventoryWhenPaymentSucceeds() {

        // Given
        TestPaymentContext context =
                createPendingPayment();

        PaymentWebhookRequest request =
                new PaymentWebhookRequest(
                        "charge.succeeded",
                        "test-public-key",
                        "https://api.unzer.test/payment",
                        context.providerPaymentId()
                );

        // When
        reconciliationService.reconcile(
                request,
                PaymentProviderStatus.SUCCEEDED
        );

        // Then
        Payment payment =
                paymentRepository.findById(
                        context.payment().getId()
                ).orElseThrow();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);

        Order order =
                orderRepository.findById(
                        context.order().getId()
                ).orElseThrow();

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.PAID);

        Inventory inventory =
                inventoryRepository.findByProductId(1L)
                        .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(8);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(0);

        List<InventoryReservation> reservations =
                reservationRepository.findByOrderIdAndStatus(
                        context.order().getId(),
                        ReservationStatus.CONFIRMED
                );

        assertThat(reservations)
                .hasSize(1);

        assertThat(processedWebhookRepository.count())
                .isEqualTo(1);
    }

    @Test
    void shouldReleaseInventoryWhenPaymentFails() {

        // Given
        TestPaymentContext context =
                createPendingPayment();

        PaymentWebhookRequest request =
                new PaymentWebhookRequest(
                        "charge.failed",
                        "test-public-key",
                        "https://api.unzer.test/payment",
                        context.providerPaymentId()
                );

        // When
        reconciliationService.reconcile(
                request,
                PaymentProviderStatus.FAILED
        );

        // Then
        Payment payment =
                paymentRepository.findById(
                        context.payment().getId()
                ).orElseThrow();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.FAILED);

        Order order =
                orderRepository.findById(
                        context.order().getId()
                ).orElseThrow();

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.PAYMENT_FAILED);

        Inventory inventory =
                inventoryRepository.findByProductId(1L)
                        .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(10);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(0);

        List<InventoryReservation> reservations =
                reservationRepository.findByOrderIdAndStatus(
                        context.order().getId(),
                        ReservationStatus.RELEASED
                );

        assertThat(reservations)
                .hasSize(1);

        assertThat(processedWebhookRepository.count())
                .isEqualTo(1);
    }

    @Test
    void shouldIgnoreDuplicateWebhook() {

        // Given
        TestPaymentContext context =
                createPendingPayment();

        PaymentWebhookRequest request =
                new PaymentWebhookRequest(
                        "charge.succeeded",
                        "test-public-key",
                        "https://api.unzer.test/payment",
                        context.providerPaymentId()
                );

        // When
        reconciliationService.reconcile(
                request,
                PaymentProviderStatus.SUCCEEDED
        );

        reconciliationService.reconcile(
                request,
                PaymentProviderStatus.SUCCEEDED
        );

        // Then
        Payment payment =
                paymentRepository.findById(
                        context.payment().getId()
                ).orElseThrow();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);

        Order order =
                orderRepository.findById(
                        context.order().getId()
                ).orElseThrow();

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.PAID);

        Inventory inventory =
                inventoryRepository.findByProductId(1L)
                        .orElseThrow();

        /*
         * The inventory must be confirmed only once.
         *
         * Original:
         * available = 10
         * reserved  = 0
         *
         * Checkout:
         * available = 8
         * reserved  = 2
         *
         * First webhook:
         * available = 8
         * reserved  = 0
         *
         * Duplicate webhook:
         * no further change
         */
        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(8);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(0);

        assertThat(processedWebhookRepository.count())
                .isEqualTo(1);
    }

    private TestPaymentContext createPendingPayment() {

        Product product =
                productRepository.findByIdAndActiveTrue(1L)
                        .orElseThrow();

        /*
         * 1. Create order.
         */
        Order order =
                orderService.createOrder(
                        product,
                        2
                );

        /*
         * 2. Reserve inventory.
         *
         * available: 10 -> 8
         * reserved:   0 -> 2
         */
        inventoryService.reserve(
                order.getId(),
                product.getId(),
                2
        );

        /*
         * 3. Order is now waiting for payment.
         */
        orderService.markAwaitingPayment(
                order.getId()
        );

        Order updatedOrder =
                orderService.getOrder(
                        order.getId()
                );

        /*
         * 4. Create local pending payment.
         */
        Payment payment =
                paymentService.createPayment(
                        updatedOrder,
                        PaymentMethod.CREDIT_CARD,
                        "checkout-" + updatedOrder.getId()
                );

        /*
         * Pretend these values were returned from Unzer.
         */
        String providerPaymentId =
                "s-pay-" + updatedOrder.getId();

        payment.attachProviderReferences(
                "s-crd-test",
                providerPaymentId,
                "s-chg-test-" + updatedOrder.getId()
        );

        return new TestPaymentContext(
                updatedOrder,
                payment,
                providerPaymentId
        );
    }

    private record TestPaymentContext(
            Order order,
            Payment payment,
            String providerPaymentId
    ) {
    }
}