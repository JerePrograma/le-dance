ALTER TABLE mensualidades
    ADD COLUMN es_clon BOOLEAN DEFAULT false;
ALTER TABLE detalle_pagos
    ADD COLUMN es_clon BOOLEAN DEFAULT false;
