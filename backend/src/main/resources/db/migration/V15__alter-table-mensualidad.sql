ALTER TABLE mensualidades
    ADD COLUMN descripcion VARCHAR(50);

UPDATE mensualidades m
SET descripcion = d.nombre || ' - CUOTA - ' || upper(to_char(m.fecha_generacion, 'TMMonth')) || ' DE ' ||
                  to_char(m.fecha_generacion, 'YYYY')
FROM inscripciones i
         JOIN disciplinas d ON i.disciplina_id = d.id
WHERE i.id = m.inscripcion_id;
