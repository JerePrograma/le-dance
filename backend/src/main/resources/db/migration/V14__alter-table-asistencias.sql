--------------------------------------------------------------------------------
-- 1. Crear la nueva tabla: asistencias_alumno_mensual
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS asistencias_alumno_mensual
(
    id                    SERIAL PRIMARY KEY,
    inscripcion_id        BIGINT NOT NULL,
    observacion           TEXT, -- Observación individual para el alumno en este mes
    asistencia_mensual_id BIGINT NOT NULL,
    CONSTRAINT fk_aam_inscripcion FOREIGN KEY (inscripcion_id)
        REFERENCES inscripciones (id),
    CONSTRAINT fk_aam_asistencia_mensual FOREIGN KEY (asistencia_mensual_id)
        REFERENCES asistencias_mensuales (id)
);

--------------------------------------------------------------------------------
-- 2. Migrar datos existentes: crear registros en asistencias_alumno_mensual
--------------------------------------------------------------------------------
-- Se generan registros para cada combinación distinta de (asistencia_mensual_id, alumno_id)
-- que exista en la tabla original de asistencias_diarias.
-- Nota: Se asume que en la tabla original, cada asistencia diarias tiene las columnas:
--    alumno_id y asistencia_mensual_id.
INSERT INTO asistencias_alumno_mensual (inscripcion_id, observacion, asistencia_mensual_id)
SELECT DISTINCT
    -- Se asume que existe una relación entre alumno y su inscripción; si la tabla inscripciones
    -- relaciona el alumno (por ejemplo, con la columna alumno_id), se usa ese valor.
    inscripciones.id,
    -- Se copia el valor global de observacion de la planilla (puede ser NULL)
    asistencias_mensuales.observacion,
    asistencias_mensuales.id
FROM asistencias_diarias
         JOIN asistencias_mensuales ON asistencias_diarias.asistencia_mensual_id = asistencias_mensuales.id
         JOIN inscripciones ON inscripciones.alumno_id = asistencias_diarias.alumno_id;
-- Ajusta el JOIN de inscripciones si tu modelo tiene otro identificador para la inscripción.

--------------------------------------------------------------------------------
-- 3. Actualizar la tabla asistencias_diarias
--------------------------------------------------------------------------------
-- 3.1 Eliminar la constraint antigua que hace referencia a la planilla y al alumno
ALTER TABLE asistencias_diarias
    DROP CONSTRAINT IF EXISTS fk_asistencia_mensual;

-- 3.2 Agregar la nueva columna que referenciará a la tabla asistencias_alumno_mensual
ALTER TABLE asistencias_diarias
    ADD COLUMN asistencia_alumno_mensual_id BIGINT;

-- 3.3 Actualizar la nueva columna asignándole el id correcto de la tabla asistencias_alumno_mensual.
-- Se hace mediante un UPDATE que vincula la antigua combinación (asistencia_mensual_id, alumno_id)
-- con el registro correspondiente en la nueva tabla.
UPDATE asistencias_diarias ad
SET asistencia_alumno_mensual_id = aam.id
FROM asistencias_alumno_mensual aam
         JOIN inscripciones i ON i.id = aam.inscripcion_id
WHERE ad.asistencia_mensual_id = aam.asistencia_mensual_id
  AND ad.alumno_id = i.alumno_id;
-- Revisa y ajusta el JOIN con la tabla inscripciones según tu modelo.

-- 3.4 Eliminar las columnas antiguas que ya no se usan
ALTER TABLE asistencias_diarias
    DROP COLUMN alumno_id;
ALTER TABLE asistencias_diarias
    DROP COLUMN asistencia_mensual_id;

-- 3.5 Agregar la nueva restricción foránea en asistencias_diarias
ALTER TABLE asistencias_diarias
    ADD CONSTRAINT fk_asistencia_alumno_mensual
        FOREIGN KEY (asistencia_alumno_mensual_id)
            REFERENCES asistencias_alumno_mensual (id);

--------------------------------------------------------------------------------
-- 4. Actualizar la tabla asistencias_mensuales
--------------------------------------------------------------------------------
-- Quitar la columna global "observacion" (ya que cada alumno tendrá su propia observación)
ALTER TABLE asistencias_mensuales
    DROP COLUMN IF EXISTS observacion;
