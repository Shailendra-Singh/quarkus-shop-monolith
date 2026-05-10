ALTER TABLE payments
    ADD payment_reference_id VARCHAR(255);

ALTER TABLE payments
    DROP COLUMN payment_id;