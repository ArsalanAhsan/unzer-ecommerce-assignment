CREATE TABLE payment (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,

    idempotency_key VARCHAR(100) NOT NULL,

    provider_payment_type_id VARCHAR(100),
    provider_transaction_id VARCHAR(100),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT uk_payment_idempotency_key
        UNIQUE (idempotency_key),

    CONSTRAINT chk_payment_amount
        CHECK (amount_minor >= 0)
);

CREATE INDEX idx_payment_order_id
    ON payment(order_id);

CREATE UNIQUE INDEX idx_payment_provider_transaction_id
    ON payment(provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;