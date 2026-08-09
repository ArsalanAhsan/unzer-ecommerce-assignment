package com.unzer.assignment.ecommerce.payment;

import com.unzer.assignment.ecommerce.catalog.Product;
import com.unzer.assignment.ecommerce.catalog.ProductRepository;
import com.unzer.assignment.ecommerce.inventory.InventoryRepository;
import com.unzer.assignment.ecommerce.inventory.InventoryService;
import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.order.OrderRepository;
import com.unzer.assignment.ecommerce.order.OrderService;
import com.unzer.assignment.ecommerce.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class RefundServiceIntegrationTest {

    @MockitoBean
    private PaymentProvider paymentProvider;

    @Autowired
    private RefundService refundService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

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

    @BeforeEach
    void resetInventory() {
        inventoryRepository.resetStock(
                1L,
                10,
                0
        );
    }

    @Test
    void shouldRefundSuccessfulPayment() {

        // Given
        RefundTestContext context =
                createSuccessfulPayment();

        when(paymentProvider.refund(
                anyString(),
                anyString()
        )).thenReturn(
                new RefundResult(
                        "s-cnl-test-123",
                        true
                )
        );

        // When
        RefundResponse response =
                refundService.refund(
                        context.payment().getId()
                );

        // Then
        Payment payment =
                paymentRepository.findById(
                        context.payment().getId()
                ).orElseThrow();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);

        Order order =
                orderRepository.findById(
                        context.order().getId()
                ).orElseThrow();

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.REFUNDED);

        assertThat(response.paymentId())
                .isEqualTo(payment.getId());

        assertThat(response.status())
                .isEqualTo(PaymentStatus.REFUNDED);

        assertThat(response.providerRefundId())
                .isEqualTo("s-cnl-test-123");

        /*
         * Refund is a financial operation.
         * We do NOT automatically restore inventory.
         *
         * Original stock: 10
         * Checkout reserves 2: available = 8
         * Successful payment confirms reservation:
         * available remains 8, reserved = 0
         * Refund must not change this.
         */
        var inventory =
                inventoryRepository.findByProductId(1L)
                        .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(8);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(0);
    }

    private RefundTestContext createSuccessfulPayment() {

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
         * 3. Order waits for payment.
         */
        orderService.markAwaitingPayment(
                order.getId()
        );

        /*
         * 4. Create local payment.
         */
        Order awaitingPaymentOrder =
                orderService.getOrder(
                        order.getId()
                );

        Payment payment =
                paymentService.createPayment(
                        awaitingPaymentOrder,
                        PaymentMethod.CREDIT_CARD,
                        "checkout-" + awaitingPaymentOrder.getId()
                );

        /*
         * Pretend these IDs came back from Unzer.
         */
        payment.attachProviderReferences(
                "s-crd-test",
                "s-pay-test-" + order.getId(),
                "s-chg-test-" + order.getId()
        );

        /*
         * 5. Simulate successful payment reconciliation.
         */
        payment.markSucceeded();

        orderService.markPaid(
                order.getId()
        );

        inventoryService.confirm(
                order.getId()
        );

        return new RefundTestContext(
                order,
                payment
        );
    }

    @Test
    void shouldRejectRefundForPendingPayment() {

        // Given
        Product product =
                productRepository.findByIdAndActiveTrue(1L)
                        .orElseThrow();

        Order order =
                orderService.createOrder(
                        product,
                        1
                );

        inventoryService.reserve(
                order.getId(),
                product.getId(),
                1
        );

        orderService.markAwaitingPayment(
                order.getId()
        );

        Order awaitingPaymentOrder =
                orderService.getOrder(
                        order.getId()
                );

        Payment payment =
                paymentService.createPayment(
                        awaitingPaymentOrder,
                        PaymentMethod.CREDIT_CARD,
                        "checkout-" + awaitingPaymentOrder.getId()
                );

        // Payment is still PENDING.

        // When + Then
        assertThatThrownBy(() ->
                refundService.refund(
                        payment.getId()
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "Only successful payments can be refunded"
                );

        Payment unchangedPayment =
                paymentRepository.findById(
                        payment.getId()
                ).orElseThrow();

        assertThat(unchangedPayment.getStatus())
                .isEqualTo(PaymentStatus.PENDING);

        Order unchangedOrder =
                orderRepository.findById(
                        order.getId()
                ).orElseThrow();

        assertThat(unchangedOrder.getStatus())
                .isEqualTo(OrderStatus.AWAITING_PAYMENT);
    }

    private record RefundTestContext(
            Order order,
            Payment payment
    ) {
    }
}