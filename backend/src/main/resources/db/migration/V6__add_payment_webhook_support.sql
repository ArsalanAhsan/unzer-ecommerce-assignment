ALTER TABLE payment
    ADD COLUMN provider_payment_id VARCHAR(100);

CREATE UNIQUE INDEX idx_payment_provider_payment_id
    ON payment(provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;


CREATE TABLE processed_webhook (
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(255) NOT NULL UNIQUE,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);