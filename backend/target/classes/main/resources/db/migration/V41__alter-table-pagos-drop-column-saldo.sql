-- 1) Quitar la constraint de la tabla 'pagos' si existe (ajusta el nombre si difiere en tu BD)
ALTER TABLE pagos
    DROP CONSTRAINT IF EXISTS fk_pagos_inscripcion_id;

-- 2) Eliminar la columna inscripcion_id
ALTER TABLE pagos
    DROP COLUMN IF EXISTS inscripcion_id;

-- 3) Eliminar la columna saldo_a_favor
ALTER TABLE pagos
    DROP COLUMN IF EXISTS saldo_a_favor;

-- 4) Eliminar la columna recargoAplicado
ALTER TABLE pagos
    DROP COLUMN IF EXISTS recargo_aplicado;

-- 5) Eliminar la columna bonificacionAplicada
ALTER TABLE pagos
    DROP COLUMN IF EXISTS bonificacion_aplicada;

ALTER TABLE pagos
    RENAME COLUMN monto_base_pago TO importe_inicial;

ALTER TABLE pagos
    ADD COLUMN valor_base BIGINT;

ALTER TABLE pagos
    DROP COLUMN IF EXISTS tipo_pago;

-- 1) Eliminar columna a_favor
ALTER TABLE detalle_pagos
    DROP COLUMN IF EXISTS a_favor;

-- 2) Eliminar columna monto_original
ALTER TABLE detalle_pagos
    RENAME COLUMN monto_original TO valor_base;

-- 3) Renombrar columna 'cuotaOCantidad' a 'cuota_ocantidad'
ALTER TABLE detalle_pagos
    RENAME COLUMN cuota TO cuota_o_cantidad;