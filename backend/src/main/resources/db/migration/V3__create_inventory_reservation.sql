CREATE TABLE inventory_reservation (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_inventory_reservation_quantity
        CHECK (quantity > 0),

    CONSTRAINT fk_inventory_reservation_product
        FOREIGN KEY (product_id)
        REFERENCES product(id)
);

CREATE INDEX idx_inventory_reservation_order
    ON inventory_reservation(order_id);

CREATE INDEX idx_inventory_reservation_status_expiry
    ON inventory_reservation(status, expires_at);