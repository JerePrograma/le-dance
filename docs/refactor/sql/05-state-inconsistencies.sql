-- Estados incompatibles con importes, fechas o bajas en V060.
WITH
active_zero_balance AS (
    SELECT id::text AS example_id, 'id=' || id || ',saldo=' || saldo_restante AS observed_value
    FROM pagos WHERE estado_pago = 'ACTIVO' AND saldo_restante = 0
),
historical_positive_balance AS (
    SELECT id::text AS example_id, 'id=' || id || ',saldo=' || saldo_restante AS observed_value
    FROM pagos WHERE estado_pago = 'HISTORICO' AND saldo_restante > 0
),
cancelled_with_active_details AS (
    SELECT p.id::text AS example_id,
           'pago=' || p.id || ',detalles_activos=' || COUNT(d.id) AS observed_value
    FROM pagos p
    JOIN detalle_pagos d ON d.pago_id = p.id
    WHERE p.estado_pago = 'ANULADO'
      AND d.estado_pago = 'ACTIVO'
      AND COALESCE(d.removido, false) = false
    GROUP BY p.id
),
paid_monthly_with_balance AS (
    SELECT id::text AS example_id, 'id=' || id || ',pendiente=' || importe_pendiente AS observed_value
    FROM mensualidades WHERE estado = 'PAGADO' AND COALESCE(importe_pendiente, 0) > 0
),
pending_monthly_without_balance AS (
    SELECT id::text AS example_id, 'id=' || id || ',pendiente=' || importe_pendiente AS observed_value
    FROM mensualidades WHERE estado = 'PENDIENTE' AND importe_pendiente = 0
),
payment_date_state_mismatch AS (
    SELECT id::text AS example_id,
           'id=' || id || ',estado=' || estado || ',fecha_pago=' || COALESCE(fecha_pago::text, 'NULL') AS observed_value
    FROM mensualidades
    WHERE (estado = 'PAGADO' AND fecha_pago IS NULL)
       OR (estado <> 'PAGADO' AND fecha_pago IS NOT NULL)
),
inactive_student_new_operations AS (
    SELECT 'PAGO:' || p.id AS example_id,
           'alumno=' || a.id || ',baja=' || COALESCE(a.fecha_de_baja::text, 'NULL') || ',operacion=' || p.fecha AS observed_value
    FROM alumnos a JOIN pagos p ON p.alumno_id = a.id
    WHERE NOT a.activo AND (a.fecha_de_baja IS NULL OR p.fecha > a.fecha_de_baja)
    UNION ALL
    SELECT 'INSCRIPCION:' || i.id,
           'alumno=' || a.id || ',baja=' || COALESCE(a.fecha_de_baja::text, 'NULL') || ',operacion=' || i.fecha_inscripcion
    FROM alumnos a JOIN inscripciones i ON i.alumno_id = a.id
    WHERE NOT a.activo AND (a.fecha_de_baja IS NULL OR i.fecha_inscripcion > a.fecha_de_baja)
    UNION ALL
    SELECT 'MENSUALIDAD:' || m.id,
           'alumno=' || a.id || ',baja=' || COALESCE(a.fecha_de_baja::text, 'NULL') || ',operacion=' || m.fecha_generacion
    FROM alumnos a
    JOIN inscripciones i ON i.alumno_id = a.id
    JOIN mensualidades m ON m.inscripcion_id = i.id
    WHERE NOT a.activo AND (a.fecha_de_baja IS NULL OR m.fecha_generacion > a.fecha_de_baja)
),
inactive_enrollment_new_obligations AS (
    SELECT 'MENSUALIDAD:' || m.id AS example_id,
           'inscripcion=' || i.id || ',baja=' || COALESCE(i.fecha_baja::text, 'NULL') ||
           ',generacion=' || m.fecha_generacion AS observed_value
    FROM inscripciones i JOIN mensualidades m ON m.inscripcion_id = i.id
    WHERE i.estado <> 'ACTIVA'
      AND (i.fecha_baja IS NULL OR m.fecha_generacion > i.fecha_baja OR m.fecha_cuota > i.fecha_baja)
    UNION ALL
    SELECT 'ASISTENCIA:' || aam.id,
           'inscripcion=' || i.id || ',baja=' || COALESCE(i.fecha_baja::text, 'NULL') ||
           ',periodo=' || make_date(am.anio, am.mes, 1)
    FROM inscripciones i
    JOIN asistencias_alumno_mensual aam ON aam.inscripcion_id = i.id
    JOIN asistencias_mensuales am ON am.id = aam.asistencia_mensual_id
    WHERE i.estado <> 'ACTIVA'
      AND (i.fecha_baja IS NULL OR make_date(am.anio, am.mes, 1) > date_trunc('month', i.fecha_baja)::date)
),
removed_but_active AS (
    SELECT 'DETALLE:' || id AS example_id,
           'removido=' || COALESCE(removido, false) || ',estado=' || estado_pago AS observed_value
    FROM detalle_pagos WHERE COALESCE(removido, false) AND estado_pago = 'ACTIVO'
    UNION ALL
    SELECT 'ALUMNO:' || id, 'activo=true,fecha_baja=' || fecha_de_baja
    FROM alumnos WHERE activo AND fecha_de_baja IS NOT NULL
    UNION ALL
    SELECT 'INSCRIPCION:' || id, 'estado=ACTIVA,fecha_baja=' || fecha_baja
    FROM inscripciones WHERE estado = 'ACTIVA' AND fecha_baja IS NOT NULL
),
scheduler_without_records AS (
    SELECT pe.id::text AS example_id,
           'proceso=' || pe.proceso || ',fecha=' || pe.ultima_ejecucion || ',tipo=MENSUALIDAD' AS observed_value
    FROM procesos_ejecutados pe
    WHERE pe.ultima_ejecucion IS NOT NULL
      AND lower(pe.proceso) LIKE '%mensual%'
      AND NOT EXISTS (SELECT 1 FROM mensualidades m WHERE m.fecha_generacion = pe.ultima_ejecucion)
    UNION ALL
    SELECT pe.id::text,
           'proceso=' || pe.proceso || ',fecha=' || pe.ultima_ejecucion || ',tipo=ASISTENCIA'
    FROM procesos_ejecutados pe
    WHERE pe.ultima_ejecucion IS NOT NULL
      AND lower(pe.proceso) LIKE '%asistencia%'
      AND NOT EXISTS (SELECT 1 FROM asistencias_diarias ad WHERE ad.fecha = pe.ultima_ejecucion)
      AND NOT EXISTS (
          SELECT 1 FROM asistencias_mensuales am
          WHERE am.anio = EXTRACT(YEAR FROM pe.ultima_ejecucion)
            AND am.mes = EXTRACT(MONTH FROM pe.ultima_ejecucion))
    UNION ALL
    SELECT pe.id::text,
           'proceso=' || pe.proceso || ',fecha=' || pe.ultima_ejecucion || ',tipo=MATRICULA'
    FROM procesos_ejecutados pe
    WHERE pe.ultima_ejecucion IS NOT NULL
      AND lower(pe.proceso) LIKE '%matricul%'
      AND NOT EXISTS (
          SELECT 1 FROM matriculas m WHERE m.anio = EXTRACT(YEAR FROM pe.ultima_ejecucion))
    UNION ALL
    SELECT pe.id::text,
           'proceso=' || pe.proceso || ',fecha=' || pe.ultima_ejecucion || ',tipo=RECARGO_SIN_TRAZA'
    FROM procesos_ejecutados pe
    WHERE pe.ultima_ejecucion IS NOT NULL AND lower(pe.proceso) LIKE '%recargo%'
)
SELECT 'STATE-PAGO-ACTIVO-CERO' AS rule_id, 'pagos' AS aggregate_name,
       COUNT(*)::bigint AS affected_count,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), '') AS sample_ids,
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), '') AS observed_values,
       'ACTIVO implica saldo_restante positivo' AS violated_rule,
       'La deuda aparece abierta sin saldo' AS impact,
       'ESTADO FINANCIERO' AS classification,
       'inequivoca' AS repairability
FROM active_zero_balance
UNION ALL
SELECT 'STATE-PAGO-HISTORICO-POSITIVO', 'pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'HISTORICO no conserva saldo positivo', 'Deuda pendiente oculta', 'ESTADO FINANCIERO', 'requiere decision'
FROM historical_positive_balance
UNION ALL
SELECT 'STATE-PAGO-ANULADO-DETALLE-ACTIVO', 'pagos/detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'ANULADO no mantiene detalles activos', 'El detalle puede seguir afectando deuda o reportes', 'ESTADO FINANCIERO', 'requiere decision'
FROM cancelled_with_active_details
UNION ALL
SELECT 'STATE-MENSUALIDAD-PAGADA-PENDIENTE', 'mensualidades', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'PAGADO implica importe_pendiente cero', 'Deuda positiva marcada cobrada', 'ESTADO FINANCIERO', 'requiere decision'
FROM paid_monthly_with_balance
UNION ALL
SELECT 'STATE-MENSUALIDAD-PENDIENTE-CERO', 'mensualidades', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'PENDIENTE implica importe_pendiente positivo', 'Obligacion saldada permanece abierta', 'ESTADO FINANCIERO', 'inequivoca'
FROM pending_monthly_without_balance
UNION ALL
SELECT 'STATE-FECHA-PAGO', 'mensualidades', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'fecha_pago existe si y solo si la mensualidad esta PAGADO', 'Cronologia financiera contradictoria', 'ESTADO/FECHA', 'requiere decision'
FROM payment_date_state_mismatch
UNION ALL
SELECT 'STATE-ALUMNO-INACTIVO-OPERACION-POSTERIOR', 'alumnos/operaciones', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Alumno inactivo no recibe operaciones nuevas posteriores a su baja', 'Se genera historial nuevo fuera de vigencia', 'VIGENCIA', 'requiere decision'
FROM inactive_student_new_operations
UNION ALL
SELECT 'STATE-INSCRIPCION-INACTIVA-OBLIGACION-POSTERIOR', 'inscripciones/obligaciones', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Inscripcion inactiva no genera obligaciones o asistencias posteriores a fecha_baja', 'Deuda o asistencia fuera de vigencia', 'VIGENCIA', 'requiere decision'
FROM inactive_enrollment_new_obligations
UNION ALL
SELECT 'STATE-REMOVIDO-ACTIVO', 'multiples_tablas', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Una baja o remocion no conserva estado activo contradictorio', 'Filtros y reportes pueden incluir datos removidos', 'ESTADO/VIGENCIA', 'inequivoca'
FROM removed_but_active
UNION ALL
SELECT 'STATE-SCHEDULER-SIN-REGISTROS', 'procesos_ejecutados', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Una marca de scheduler se corresponde con registros observables del proceso',
       'La marca puede ocultar ejecucion parcial; recargos no tienen traza durable en V060', 'IDEMPOTENCIA', 'requiere decision'
FROM scheduler_without_records
ORDER BY rule_id;
