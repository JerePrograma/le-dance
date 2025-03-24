ALTER TABLE mensualidades
    ADD COLUMN IF NOT EXISTS monto_abonado numeric NOT NULL DEFAULT 0.0;