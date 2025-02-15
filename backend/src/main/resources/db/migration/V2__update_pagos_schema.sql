-- V2__update_pagos_schema.sql
-- 1) Convertir la columna "bonificacion_aplicada" de boolean a NUMERIC(10,2)
ALTER TABLE pagos
    ALTER COLUMN bonificacion_aplicada DROP DEFAULT,
    ALTER COLUMN bonificacion_aplicada TYPE NUMERIC(10,2),
    ALTER COLUMN bonificacion_aplicada SET DEFAULT 0.0;

-- 2) Agregar la columna "saldo_a_favor" (ya que es nueva en la entidad Pago)
ALTER TABLE pagos
    ADD COLUMN saldo_a_favor NUMERIC(10,2) NOT NULL DEFAULT 0.0;
