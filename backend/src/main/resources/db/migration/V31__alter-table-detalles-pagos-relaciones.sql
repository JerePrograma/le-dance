ALTER TABLE detalle_pagos
    ADD COLUMN concepto_id    INT,
    ADD COLUMN subconcepto_id INT,
    ADD COLUMN mensualidad_id INT,
    ADD COLUMN matricula_id   INT;

ALTER TABLE detalle_pagos
    ADD CONSTRAINT fk_detallepago_concepto
        FOREIGN KEY (concepto_id) REFERENCES conceptos (id),
    ADD CONSTRAINT fk_detallepago_subconcepto
        FOREIGN KEY (subconcepto_id) REFERENCES sub_conceptos (id),
    ADD CONSTRAINT fk_detallepago_mensualidad
        FOREIGN KEY (mensualidad_id) REFERENCES mensualidades (id),
    ADD CONSTRAINT fk_detallepago_matricula
        FOREIGN KEY (matricula_id) REFERENCES matriculas (id);

CREATE INDEX idx_detallepago_concepto ON detalle_pagos (concepto_id);
CREATE INDEX idx_detallepago_subconcepto ON detalle_pagos (subconcepto_id);
