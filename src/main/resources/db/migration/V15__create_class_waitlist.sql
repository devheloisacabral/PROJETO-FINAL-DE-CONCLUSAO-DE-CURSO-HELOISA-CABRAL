CREATE TABLE class_waitlist (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ativa',
    CONSTRAINT fk_class_waitlist_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_class_waitlist_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT ck_class_waitlist_status CHECK (status IN ('ativa', 'promovida', 'cancelada'))
);

CREATE INDEX idx_class_waitlist_class_order ON class_waitlist (class_id, status, requested_at, id);
CREATE UNIQUE INDEX uk_active_class_waitlist_student
    ON class_waitlist (student_id, class_id)
    WHERE status = 'ativa';
