-- Insertar el SubConcepto "Matricula" si no existe
INSERT INTO sub_conceptos (descripcion)
SELECT 'Matricula'
WHERE NOT EXISTS (SELECT 1 FROM sub_conceptos WHERE descripcion = 'Matricula');

-- Insertar el Concepto "Matricula" asociado al SubConcepto "Matricula"
INSERT INTO conceptos (descripcion, precio, sub_concepto_id)
SELECT 'Matricula', 0.0, id
FROM sub_conceptos
WHERE descripcion = 'Matricula';

-- Insertar los Salones "Salon 1", "Salon 2" y "Salon 3"
INSERT INTO salones (nombre, descripcion)
VALUES ('Salon 1', 'Primer salon de clases'),
       ('Salon 2', 'Segundo salon de clases'),
       ('Salon 3', 'Tercer salon de clases');