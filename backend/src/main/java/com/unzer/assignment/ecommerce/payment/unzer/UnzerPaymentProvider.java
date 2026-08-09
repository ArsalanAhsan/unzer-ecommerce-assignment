package com.unzer.assignment.ecommerce.payment.unzer;

import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.payment.PaymentMethod;
import com.unzer.assignment.ecommerce.payment.PaymentProvider;
import com.unzer.assignment.ecommerce.payment.PaymentStartResult;
import com.unzer.payment.Unzer;
import com.unzer.payment.communication.HttpCommunicationException;
import com.unzer.payment.paymenttypes.Wero;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Currency;

@Component
public class UnzerPaymentProvider implements PaymentProvider {

    private final String privateKey;
    private final String returnUrl;

    public UnzerPaymentProvider(
            @Value("${unzer.private-key:}") String privateKey,
            @Value("${unzer.return-url:http://localhost:8080/api/v1/payments/return}")
            String returnUrl
    ) {
        this.privateKey = privateKey;
        this.returnUrl = returnUrl;
    }

    @Override
    public PaymentStartResult startPayment(
            Order order,
            PaymentMethod method,
            String paymentTypeId
    ) {

        validateConfiguration();

        if (method == null) {
            throw new IllegalArgumentException(
                    "Payment method is required"
            );
        }

        return switch (method) {

            case CREDIT_CARD -> {

                if (paymentTypeId == null || paymentTypeId.isBlank()) {
                    throw new IllegalArgumentException(
                            "paymentTypeId is required for credit card payments"
                    );
                }

                yield startCreditCardPayment(
                        order,
                        paymentTypeId
                );
            }

            case WERO ->
                    startWeroPayment(order);

            case OPEN_BANKING ->
                    throw new UnsupportedOperationException(
                            "Open Banking is not implemented in this vertical slice"
                    );
        };
    }

    private PaymentStartResult startCreditCardPayment(
            Order order,
            String paymentTypeId
    ) {

        Unzer unzer = createClient();

        BigDecimal amount =
                toMajorUnits(
                        order.getTotalAmountMinor()
                );

        try {

            URL callbackUrl = createReturnUrl();

            var charge = unzer.charge(
                    amount,
                    Currency.getInstance(order.getCurrency()),
                    paymentTypeId,
                    callbackUrl
            );

            return new PaymentStartResult(
                    paymentTypeId,
                    charge.getId(),
                    charge.getRedirectUrl() != null
                            ? charge.getRedirectUrl().toString()
                            : null
            );

        } catch (HttpCommunicationException exception) {

            throw new IllegalStateException(
                    "Unzer credit card payment request failed",
                    exception
            );
        }
    }

    private PaymentStartResult startWeroPayment(
            Order order
    ) {

        Unzer unzer = createClient();

        BigDecimal amount =
                toMajorUnits(
                        order.getTotalAmountMinor()
                );

        try {

            URL callbackUrl = createReturnUrl();

            Wero wero =
                    unzer.createPaymentType(
                            new Wero()
                    );

            var charge = unzer.charge(
                    amount,
                    Currency.getInstance(order.getCurrency()),
                    wero.getId(),
                    callbackUrl
            );

            return new PaymentStartResult(
                    wero.getId(),
                    charge.getId(),
                    charge.getRedirectUrl() != null
                            ? charge.getRedirectUrl().toString()
                            : null
            );

        } catch (HttpCommunicationException exception) {

            throw new IllegalStateException(
                    "Unzer Wero payment request failed",
                    exception
            );
        }
    }

    private BigDecimal toMajorUnits(
            Long amountMinor
    ) {
        return BigDecimal.valueOf(
                amountMinor,
                2
        );
    }

    private URL createReturnUrl() {

        try {
            return URI.create(returnUrl)
                    .toURL();

        } catch (IllegalArgumentException |
                 MalformedURLException exception) {

            throw new IllegalStateException(
                    "Invalid Unzer return URL: " + returnUrl,
                    exception
            );
        }
    }

    private Unzer createClient() {
        return new Unzer(privateKey);
    }

    private void validateConfiguration() {

        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException(
                    "UNZER_PRIVATE_KEY is not configured"
            );
        }
    }
}