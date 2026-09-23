CREATE TABLE monthly_fee_refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_monthly_fee_refunds_payment FOREIGN KEY (payment_id) REFERENCES monthly_fee_payments (id),
    CONSTRAINT ck_monthly_fee_refunds_amount_positive CHECK (amount > 0),
    CONSTRAINT uk_monthly_fee_refunds_payment UNIQUE (payment_id)
);

CREATE INDEX idx_monthly_fee_refunds_payment_id ON monthly_fee_refunds (payment_id);
