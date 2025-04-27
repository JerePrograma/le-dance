ALTER TABLE mensualidades
    ADD COLUMN fecha_pago DATE;

UPDATE mensualidades m
SET descripcion = (SELECT d.nombre
                   FROM inscripciones i
                            JOIN disciplinas d ON i.disciplina_id = d.id
                   WHERE i.id = m.inscripcion_id) || ' - CUOTA - ' || upper(to_char(m.fecha_generacion, 'TMMonth')) ||
                  ' DE ' ||
                  to_char(m.fecha_generacion, 'YYYY');