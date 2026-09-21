ALTER TABLE users
    ADD COLUMN name VARCHAR(120),
    ADD COLUMN phone VARCHAR(30),
    ADD COLUMN birth_date DATE,
    ADD COLUMN status VARCHAR(20);

ALTER TABLE users
    ADD CONSTRAINT ck_users_student_status CHECK (status IS NULL OR status IN ('inativo'));
