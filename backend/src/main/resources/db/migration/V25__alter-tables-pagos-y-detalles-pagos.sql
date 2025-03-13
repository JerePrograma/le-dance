-- Agregar columnas en la tabla detalle_pagos para almacenar el importe original y el pendiente
ALTER TABLE detalle_pagos
    ADD COLUMN importe_inicial   NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN importe_pendiente NUMERIC(10, 2) NOT NULL DEFAULT 0.00;

-- Actualizar las nuevas columnas con el valor actual de la columna 'importe'
UPDATE detalle_pagos
SET importe_inicial   = importe,
    importe_pendiente = importe;

-- (Opcional) Si deseas agregar restricciones o índices adicionales para auditoría o performance,
-- puedes incluirlos a continuación. Por ejemplo, para asegurar que los importes nunca sean negativos:
ALTER TABLE detalle_pagos
    ADD CONSTRAINT chk_importe_inicial_nonnegative CHECK (importe_inicial >= 0),
    ADD CONSTRAINT chk_importe_pendiente_nonnegative CHECK (importe_pendiente >= 0);

-- Si en la entidad Pago deseas asegurarte de que los montos (monto, monto_pagado, saldo_restante)
-- se mantengan consistentes, revisa que ya cuenten con las restricciones y tipos adecuados.
-- Por ejemplo, si la tabla pagos se creó anteriormente, puedes verificar o ajustar:
ALTER TABLE pagos
    ALTER COLUMN monto TYPE NUMERIC(10, 2),
    ALTER COLUMN monto_pagado TYPE NUMERIC(10, 2),
    ALTER COLUMN saldo_restante TYPE NUMERIC(10, 2),
    ALTER COLUMN saldo_a_favor TYPE NUMERIC(10, 2);

-- Asegurarse de que no se permitan valores negativos en los montos (esto refuerza la consistencia):
ALTER TABLE pagos
    ADD CONSTRAINT chk_monto_nonnegative CHECK (monto >= 0),
    ADD CONSTRAINT chk_monto_pagado_nonnegative CHECK (monto_pagado >= 0),
    ADD CONSTRAINT chk_saldo_restante_nonnegative CHECK (saldo_restante >= 0),
    ADD CONSTRAINT chk_saldo_a_favor_nonnegative CHECK (saldo_a_favor >= 0);
