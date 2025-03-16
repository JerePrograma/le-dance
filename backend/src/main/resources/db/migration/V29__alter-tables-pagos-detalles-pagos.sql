-- 1. Renombrar la columna "valorBase" a "montoOriginal" en la tabla "detalle_pagos"
ALTER TABLE detalle_pagos
    RENAME COLUMN valor_base TO monto_original;

-- 2. Agregar la columna "montoBasePago" a la tabla "pagos"
-- Se utiliza IF NOT EXISTS para evitar errores si la columna ya existe
ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS monto_base_pago DOUBLE PRECISION DEFAULT 0.0;

-- 3. Actualizar los registros existentes en "pagos" para que "montoBasePago" tenga el valor actual de "monto"
UPDATE pagos
SET monto_base_pago = monto;
