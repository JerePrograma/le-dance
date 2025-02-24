-- V8__create_conceptos_and_sub_conceptos.sql
-- Creación de las tablas "sub_conceptos" y "conceptos"

-- 1. Crear tabla "sub_conceptos"
CREATE TABLE sub_conceptos
(
    id          SERIAL PRIMARY KEY,
    descripcion VARCHAR(255) NOT NULL
);

-- 2. Crear tabla "conceptos"
CREATE TABLE conceptos
(
    id              SERIAL PRIMARY KEY,
    descripcion     VARCHAR(255)   NOT NULL,
    precio          NUMERIC(10, 2) NOT NULL,
    sub_concepto_id INTEGER        NOT NULL,
    CONSTRAINT fk_sub_concepto FOREIGN KEY (sub_concepto_id)
        REFERENCES sub_conceptos (id)
        ON DELETE RESTRICT
);
