CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL,
    lesson_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lessons_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT ck_lessons_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_lessons_class ON lessons (class_id);
CREATE INDEX idx_lessons_date ON lessons (lesson_date);
