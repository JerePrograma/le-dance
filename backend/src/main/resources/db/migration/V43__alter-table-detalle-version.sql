ALTER TABLE detalle_pagos
    ALTER COLUMN version DROP DEFAULT;

ALTER TABLE detalle_pagos
    ALTER COLUMN version DROP NOT NULL;
