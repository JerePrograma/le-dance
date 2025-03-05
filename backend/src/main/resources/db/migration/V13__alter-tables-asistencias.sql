-- Paso 1: Agregar la columna como nullable
ALTER TABLE asistencias_mensuales
    ADD COLUMN disciplina_id BIGINT;

-- Paso 2: Actualizar la columna con los datos existentes
UPDATE asistencias_mensuales am
SET disciplina_id = i.disciplina_id
FROM inscripciones i
WHERE i.id = am.inscripcion_id;

-- Verifica que no hayan valores nulos (opcional, por ejemplo: SELECT * FROM asistencias_mensuales WHERE disciplina_id IS NULL;)

-- Paso 3: Cambiar la columna para que sea NOT NULL
ALTER TABLE asistencias_mensuales
    ALTER COLUMN disciplina_id SET NOT NULL;

-- Paso 4: Eliminar la restricción antigua (si sigue existiendo) y la columna inscripcion_id
ALTER TABLE asistencias_mensuales
    DROP CONSTRAINT IF EXISTS fk_inscripcion;

ALTER TABLE asistencias_mensuales
    DROP COLUMN IF EXISTS inscripcion_id;

-- Paso 5: Agregar la nueva restricción de clave foránea
ALTER TABLE asistencias_mensuales
    ADD CONSTRAINT fk_disciplina
        FOREIGN KEY (disciplina_id) REFERENCES disciplinas (id);
