package com.unzer.assignment.ecommerce.order;

import com.unzer.assignment.ecommerce.catalog.Product;
import com.unzer.assignment.ecommerce.catalog.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldCreateOrderWithPriceSnapshot() {

        // Given
        Product product = productRepository
                .findByIdAndActiveTrue(1L)
                .orElseThrow();

        // When
        Order order = orderService.createOrder(
                product,
                2
        );

        // Then
        Order savedOrder = orderRepository
                .findById(order.getId())
                .orElseThrow();

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(savedOrder.getTotalAmountMinor())
                .isEqualTo(5998L);

        assertThat(savedOrder.getCurrency())
                .isEqualTo("EUR");

        assertThat(savedOrder.getItems())
                .hasSize(1);

        OrderItem item = savedOrder.getItems().get(0);

        assertThat(item.getProductId())
                .isEqualTo(1L);

        assertThat(item.getUnitPriceMinor())
                .isEqualTo(2999L);

        assertThat(item.getQuantity())
                .isEqualTo(2);
    }
}