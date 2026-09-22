CREATE TABLE plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    CONSTRAINT ck_plans_amount_positive CHECK (amount > 0)
);

ALTER TABLE users
    ADD COLUMN plan_id BIGINT,
    ADD CONSTRAINT fk_users_plan FOREIGN KEY (plan_id) REFERENCES plans (id);

ALTER TABLE users DROP CONSTRAINT ck_users_student_status;
ALTER TABLE users ADD CONSTRAINT ck_users_student_status CHECK (status IS NULL OR status IN ('inativo', 'ativo'));

CREATE TABLE monthly_fees (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    reference_month DATE NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_monthly_fees_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT uk_monthly_fees_student_month UNIQUE (student_id, reference_month),
    CONSTRAINT ck_monthly_fees_status CHECK (status IN ('pendente', 'paga')),
    CONSTRAINT ck_monthly_fees_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_monthly_fees_student_id ON monthly_fees (student_id);
