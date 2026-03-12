ALTER TABLE usuarios
    ADD COLUMN senha_hash VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE usuarios
    ADD CONSTRAINT uk_usuarios_email UNIQUE (email);
