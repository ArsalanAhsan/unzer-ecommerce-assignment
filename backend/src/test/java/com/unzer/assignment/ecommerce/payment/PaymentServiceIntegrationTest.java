package com.unzer.assignment.ecommerce.payment;

import com.unzer.assignment.ecommerce.catalog.Product;
import com.unzer.assignment.ecommerce.catalog.ProductRepository;
import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.order.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Test
    void shouldCreatePendingPaymentIdempotently() {

        // Given
        Product product = productRepository
                .findByIdAndActiveTrue(1L)
                .orElseThrow();

        Order order = orderService.createOrder(
                product,
                1
        );

        String idempotencyKey =
                "checkout-" + order.getId();

        // When
        Payment firstPayment =
                paymentService.createPayment(
                        order,
                        PaymentMethod.CREDIT_CARD,
                        idempotencyKey
                );

        Payment secondPayment =
                paymentService.createPayment(
                        order,
                        PaymentMethod.CREDIT_CARD,
                        idempotencyKey
                );

        // Then
        assertThat(firstPayment.getId())
                .isEqualTo(secondPayment.getId());

        assertThat(firstPayment.getStatus())
                .isEqualTo(PaymentStatus.PENDING);

        assertThat(firstPayment.getAmountMinor())
                .isEqualTo(2999L);

        assertThat(firstPayment.getCurrency())
                .isEqualTo("EUR");

        assertThat(firstPayment.getOrderId())
                .isEqualTo(order.getId());

        assertThat(paymentRepository.count())
                .isEqualTo(1);
    }
}