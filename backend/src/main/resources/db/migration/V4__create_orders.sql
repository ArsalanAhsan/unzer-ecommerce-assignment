CREATE TABLE orders (
    id UUID PRIMARY KEY,
    status VARCHAR(30) NOT NULL,
    total_amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_order_total_amount
        CHECK (total_amount_minor >= 0)
);

CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    unit_price_minor BIGINT NOT NULL,
    quantity INTEGER NOT NULL,

    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id)
        REFERENCES product(id),

    CONSTRAINT chk_order_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_item_price
        CHECK (unit_price_minor >= 0)
);

CREATE INDEX idx_order_item_order_id
    ON order_item(order_id);

ALTER TABLE inventory_reservation
    ADD CONSTRAINT fk_inventory_reservation_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id);