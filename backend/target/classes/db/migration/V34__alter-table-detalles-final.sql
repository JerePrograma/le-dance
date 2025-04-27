ALTER TABLE detalle_pagos
    RENAME COLUMN codigo_concepto TO descripcion_concepto;
ALTER TABLE detalle_pagos
    DROP COLUMN importe;