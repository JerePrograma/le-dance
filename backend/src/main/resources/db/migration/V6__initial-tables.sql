-- Insertar el SubConcepto "Matrícula" si no existe
INSERT INTO sub_conceptos (descripcion)
SELECT 'Matricula'
WHERE NOT EXISTS (SELECT 1 FROM sub_conceptos WHERE descripcion = 'Matrícula');

-- Insertar el Concepto "Matrícula" asociado al SubConcepto "Matrícula"
INSERT INTO conceptos (descripcion, precio, sub_concepto_id)
SELECT 'Matricula', 0.0, id
FROM sub_conceptos
WHERE descripcion = 'Matricula';

-- Insertar los Salones "Salón 1", "Salón 2" y "Salón 3"
INSERT INTO salones (nombre, descripcion)
VALUES ('Salón 1', 'Primer salón de clases'),
       ('Salón 2', 'Segundo salón de clases'),
       ('Salón 3', 'Tercer salón de clases');