CREATE TABLE student_plan_history (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    CONSTRAINT fk_student_plan_history_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_student_plan_history_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT uk_student_plan_history_student_start UNIQUE (student_id, valid_from)
);

CREATE INDEX idx_student_plan_history_student_valid_from
    ON student_plan_history (student_id, valid_from DESC);

INSERT INTO student_plan_history (student_id, plan_id, valid_from)
SELECT id, plan_id, DATE '1970-01-01'
FROM users
WHERE plan_id IS NOT NULL;
