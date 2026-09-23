ALTER TABLE enrollments
    DROP CONSTRAINT uk_enrollments_student_class,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ativa';

ALTER TABLE enrollments
    ADD CONSTRAINT ck_enrollments_status CHECK (status IN ('ativa', 'cancelada'));

CREATE UNIQUE INDEX uk_active_enrollments_student_class
    ON enrollments (student_id, class_id)
    WHERE status = 'ativa';
