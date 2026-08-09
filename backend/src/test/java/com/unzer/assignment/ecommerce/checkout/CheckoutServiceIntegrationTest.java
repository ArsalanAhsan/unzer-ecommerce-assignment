package com.unzer.assignment.ecommerce.checkout;

import com.unzer.assignment.ecommerce.inventory.Inventory;
import com.unzer.assignment.ecommerce.inventory.InventoryRepository;
import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.order.OrderRepository;
import com.unzer.assignment.ecommerce.order.OrderStatus;
import com.unzer.assignment.ecommerce.payment.Payment;
import com.unzer.assignment.ecommerce.payment.PaymentMethod;
import com.unzer.assignment.ecommerce.payment.PaymentProvider;
import com.unzer.assignment.ecommerce.payment.PaymentRepository;
import com.unzer.assignment.ecommerce.payment.PaymentStartResult;
import com.unzer.assignment.ecommerce.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@SpringBootTest
@Transactional
class CheckoutServiceIntegrationTest {

    @MockitoBean
    private PaymentProvider paymentProvider;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void resetInventory() {
        inventoryRepository.resetStock(
                1L,
                10,
                0
        );
    }

    @Test
    void shouldCreateOrderAndReserveInventory() {

        // Given
        CheckoutRequest request =
                new CheckoutRequest(
                        1L,
                        2,
                        PaymentMethod.CREDIT_CARD,
                        "crd-test"
                );

        when(paymentProvider.startPayment(
                any(),
                any(),
                anyString()
        )).thenReturn(
                new PaymentStartResult(
                        "crd-test",
                        "txn-test-123",
                        null
                )
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
                .isEqualTo(OrderStatus.AWAITING_PAYMENT);

        assertThat(order.getTotalAmountMinor())
                .isEqualTo(5998L);

        Inventory inventory =
                inventoryRepository.findByProductId(1L)
                        .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(8);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(2);

        assertThat(paymentRepository.findAll())
                .hasSize(1);

        Payment payment =
                paymentRepository.findAll().get(0);

        assertThat(payment.getOrderId())
                .isEqualTo(response.orderId());

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.PENDING);

        assertThat(payment.getAmountMinor())
                .isEqualTo(5998L);

        assertThat(payment.getCurrency())
                .isEqualTo("EUR");

        assertThat(payment.getProviderPaymentTypeId())
                .isEqualTo("crd-test");

        assertThat(payment.getProviderTransactionId())
                .isEqualTo("txn-test-123");
    }
    @Test
    void shouldReturnRedirectUrlForWeroCheckout() {

        // Given
        CheckoutRequest request =
                new CheckoutRequest(
                        1L,
                        1,
                        PaymentMethod.WERO,
                        null
                );

        when(paymentProvider.startPayment(
                any(),
                eq(PaymentMethod.WERO),
                isNull()
        )).thenReturn(
                new PaymentStartResult(
                        "s-wro-test",
                        "txn-wero-test",
                        "https://wero.example/redirect"
                )
        );

        // When
        CheckoutResponse response =
                checkoutService.checkout(request);

        // Then
        assertThat(response.status())
                .isEqualTo(OrderStatus.AWAITING_PAYMENT);

        assertThat(response.redirectUrl())
                .isEqualTo("https://wero.example/redirect");

        Payment payment =
                paymentRepository.findById(
                        response.paymentId()
                ).orElseThrow();

        assertThat(payment.getMethod())
                .isEqualTo(PaymentMethod.WERO);

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.PENDING);

        assertThat(payment.getProviderPaymentTypeId())
                .isEqualTo("s-wro-test");

        assertThat(payment.getProviderTransactionId())
                .isEqualTo("txn-wero-test");
    }
}