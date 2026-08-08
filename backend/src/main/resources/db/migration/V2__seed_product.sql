INSERT INTO product (
    name,
    sku,
    price_minor,
    currency,
    active
)
VALUES (
    'Test T-Shirt',
    'TSHIRT-001',
    2999,
    'EUR',
    TRUE
);

INSERT INTO inventory (
    product_id,
    available_quantity,
    reserved_quantity
)
SELECT
    id,
    10,
    0
FROM product
WHERE sku = 'TSHIRT-001';