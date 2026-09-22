ALTER TABLE users
    DROP CONSTRAINT ck_users_student_status;

ALTER TABLE users
    ADD CONSTRAINT ck_users_student_status CHECK (status IS NULL OR status IN ('ativo', 'inativo'));

CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_enrollments_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT uk_enrollments_student_class UNIQUE (student_id, class_id)
);

CREATE INDEX idx_enrollments_student ON enrollments (student_id);
CREATE INDEX idx_enrollments_class ON enrollments (class_id);
