-- V2__update_pagos_schema.sql
-- 1) Convertir la columna "bonificacion_aplicada" de boolean a NUMERIC(10,2)
ALTER TABLE pagos
    ALTER COLUMN bonificacion_aplicada DROP DEFAULT,
    ALTER COLUMN bonificacion_aplicada TYPE NUMERIC(10,2),
    ALTER COLUMN bonificacion_aplicada SET DEFAULT 0.0;
