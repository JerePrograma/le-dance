DROP TABLE IF EXISTS recargo_detalles CASCADE;

ALTER TABLE detalle_pagos
    DROP COLUMN IF EXISTS recargo_detalle_id;
