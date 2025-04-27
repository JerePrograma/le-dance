WITH duplicates AS (SELECT id,
                           ROW_NUMBER() OVER (PARTITION BY alumno_id, anio ORDER BY id) AS rn
                    FROM matriculas)
DELETE
FROM matriculas
WHERE id IN (SELECT id
             FROM duplicates
             WHERE rn > 1);

ALTER TABLE matriculas
    ADD CONSTRAINT uq_alumno_anio UNIQUE (alumno_id, anio);