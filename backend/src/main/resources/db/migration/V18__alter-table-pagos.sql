ALTER TABLE pagos
    ALTER COLUMN inscripcion_id DROP NOT NULL;
ALTER TABLE pagos
    ADD COLUMN tipo_pago VARCHAR(20) NOT NULL DEFAULT 'SUBSCRIPTION';