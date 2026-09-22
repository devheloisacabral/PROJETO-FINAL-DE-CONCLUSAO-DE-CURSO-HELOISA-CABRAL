CREATE TABLE attendances (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_attendances_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_attendances_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT uk_attendances_student_lesson UNIQUE (student_id, lesson_id)
);

CREATE INDEX idx_attendances_student ON attendances (student_id);
CREATE INDEX idx_attendances_lesson ON attendances (lesson_id);
