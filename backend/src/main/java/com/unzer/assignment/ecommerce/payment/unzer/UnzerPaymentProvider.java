package com.unzer.assignment.ecommerce.payment.unzer;

import com.unzer.assignment.ecommerce.order.Order;
import com.unzer.assignment.ecommerce.payment.PaymentMethod;
import com.unzer.assignment.ecommerce.payment.PaymentProvider;
import com.unzer.assignment.ecommerce.payment.PaymentStartResult;
import com.unzer.payment.Unzer;
import com.unzer.payment.communication.HttpCommunicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
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

        if (method != PaymentMethod.CREDIT_CARD) {
            throw new UnsupportedOperationException(
                    "Payment method not implemented yet: " + method
            );
        }

        if (paymentTypeId == null || paymentTypeId.isBlank()) {
            throw new IllegalArgumentException(
                    "paymentTypeId is required for credit card payments"
            );
        }

        return startCreditCardPayment(
                order,
                paymentTypeId
        );
    }

    private PaymentStartResult startCreditCardPayment(
            Order order,
            String paymentTypeId
    ) {

        Unzer unzer = createClient();

        BigDecimal amount =
                BigDecimal.valueOf(
                        order.getTotalAmountMinor(),
                        2
                );

        try {

            var callbackUrl =
                    URI.create(returnUrl).toURL();

            var charge = unzer.charge(
                    amount,
                    Currency.getInstance(order.getCurrency()),
                    paymentTypeId,
                    callbackUrl
            );

            String transactionId =
                    charge.getId();

            String redirectUrl =
                    charge.getRedirectUrl() != null
                            ? charge.getRedirectUrl().toString()
                            : null;

            return new PaymentStartResult(
                    paymentTypeId,
                    transactionId,
                    redirectUrl
            );

        } catch (HttpCommunicationException exception) {

            throw new IllegalStateException(
                    "Unzer payment request failed",
                    exception
            );

        } catch (MalformedURLException exception) {

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