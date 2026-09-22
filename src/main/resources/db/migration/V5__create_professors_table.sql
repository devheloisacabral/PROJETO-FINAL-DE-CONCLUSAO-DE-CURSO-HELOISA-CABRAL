CREATE TABLE professors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cref VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT uk_professors_user UNIQUE (user_id),
    CONSTRAINT uk_professors_cref UNIQUE (cref),
    CONSTRAINT fk_professors_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_professors_status CHECK (status IN ('ativo', 'inativo'))
);
