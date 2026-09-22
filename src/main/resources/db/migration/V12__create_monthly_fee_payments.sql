CREATE TABLE monthly_fee_payments (
    id BIGSERIAL PRIMARY KEY,
    monthly_fee_id BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_monthly_fee_payments_fee FOREIGN KEY (monthly_fee_id) REFERENCES monthly_fees (id),
    CONSTRAINT ck_monthly_fee_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_monthly_fee_payments_status CHECK (status IN ('valido', 'cancelado'))
);

CREATE INDEX idx_monthly_fee_payments_fee_id ON monthly_fee_payments (monthly_fee_id);
