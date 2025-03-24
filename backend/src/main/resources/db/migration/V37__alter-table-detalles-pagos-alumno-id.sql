-- 1. Agregar la columna alumno_id con DEFAULT 0
ALTER TABLE detalle_pagos
    ADD COLUMN alumno_id BIGINT DEFAULT 1;

-- 2. Actualizar la columna alumno_id basándose en la relación existente en la tabla pagos
UPDATE detalle_pagos dp
SET alumno_id = p.alumno_id
FROM pagos p
WHERE dp.pago_id = p.id;

-- Si la consulta anterior no retorna filas, se procede a establecer la restricción NOT NULL:
ALTER TABLE detalle_pagos
    ALTER COLUMN alumno_id SET NOT NULL;

-- 3. Agregar la llave foránea para referenciar la tabla alumnos
ALTER TABLE detalle_pagos
    ADD CONSTRAINT fk_detalle_pago_alumno
        FOREIGN KEY (alumno_id) REFERENCES alumnos (id);