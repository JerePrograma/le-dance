ALTER TABLE detalle_pagos
    ALTER COLUMN pago_id DROP NOT NULL;

ALTER TABLE detalle_pagos
    DROP CONSTRAINT fk_detalle_pago_pago,
    ADD CONSTRAINT fk_detalle_pago_pago
        FOREIGN KEY (pago_id) REFERENCES pagos (id)
            ON DELETE SET NULL;
