ALTER TABLE detalle_pagos
    ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;