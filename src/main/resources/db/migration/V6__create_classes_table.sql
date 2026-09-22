CREATE TABLE classes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    weekday VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_capacity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    responsible_professor_id BIGINT NOT NULL,
    CONSTRAINT fk_classes_responsible_professor FOREIGN KEY (responsible_professor_id) REFERENCES professors (id),
    CONSTRAINT ck_classes_capacity_positive CHECK (max_capacity > 0),
    CONSTRAINT ck_classes_time_order CHECK (end_time > start_time),
    CONSTRAINT ck_classes_status CHECK (status IN ('ativa', 'inativa'))
);
CREATE TABLE class_professors (
    class_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    PRIMARY KEY (class_id, professor_id),
    CONSTRAINT fk_class_professors_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE CASCADE,
    CONSTRAINT fk_class_professors_professor FOREIGN KEY (professor_id) REFERENCES professors (id)
);
