ALTER TABLE detalle_pagos
    ADD COLUMN estado_pago VARCHAR(50);

UPDATE detalle_pagos
SET estado_pago = CASE
                      WHEN importe_pendiente > 0 THEN 'ACTIVO'
                      ELSE 'HISTORICO'
    END;

ALTER TABLE detalle_pagos
    ALTER COLUMN estado_pago SET NOT NULL;

ALTER TABLE detalle_pagos
    ALTER COLUMN estado_pago SET DEFAULT 'ACTIVO';
