-- V10__alter-table-asistencias.sql

-- 1. Agregar la columna "observacion" a la tabla "asistencias_mensuales" (si aún no existe)
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name='asistencias_mensuales' AND column_name='observacion'
        ) THEN
            ALTER TABLE asistencias_mensuales ADD COLUMN observacion TEXT;
        END IF;
    END
$$;

-- 2. (Opcional) Migrar datos de la antigua tabla "observacion_mensual"
-- Si la tabla "observacion_mensual" existe, se concatenan sus valores por asistencia mensual.
DO $$
    BEGIN
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'observacion_mensual') THEN
            UPDATE asistencias_mensuales am
            SET observacion = sub.observacion_concat
            FROM (
                     SELECT asistencia_mensual_id,
                            string_agg(observacion, ', ') AS observacion_concat
                     FROM observacion_mensual
                     GROUP BY asistencia_mensual_id
                 ) sub
            WHERE am.id = sub.asistencia_mensual_id;
        END IF;
    END
$$;

-- 3. Eliminar la tabla "observacion_mensual" si existe
DROP TABLE IF EXISTS observacion_mensual CASCADE;


ALTER TABLE asistencias_diarias
    DROP COLUMN observacion;