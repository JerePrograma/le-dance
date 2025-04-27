ALTER TABLE detalle_pagos
    ADD COLUMN stock_id BIGINT;

ALTER TABLE detalle_pagos
    ADD CONSTRAINT fk_detallepago_stock
        FOREIGN KEY (stock_id) REFERENCES stocks (id);

CREATE INDEX idx_detallepago_stock
    ON detalle_pagos (stock_id);