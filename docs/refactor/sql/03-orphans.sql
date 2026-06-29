-- Huerfanos y relaciones de origen ambiguas en el catalogo V060.
WITH
payments_without_student AS (
    SELECT p.id::text AS example_id
    FROM pagos p LEFT JOIN alumnos a ON a.id = p.alumno_id
    WHERE p.alumno_id IS NULL OR a.id IS NULL
),
payments_without_user AS (
    SELECT p.id::text AS example_id
    FROM pagos p LEFT JOIN usuarios u ON u.id = p.usuario_id
    WHERE p.usuario_id IS NULL OR u.id IS NULL
),
details_without_payment AS (
    SELECT d.id::text AS example_id
    FROM detalle_pagos d LEFT JOIN pagos p ON p.id = d.pago_id
    WHERE d.pago_id IS NULL OR p.id IS NULL
),
details_without_student AS (
    SELECT d.id::text AS example_id
    FROM detalle_pagos d LEFT JOIN alumnos a ON a.id = d.alumno_id
    WHERE d.alumno_id IS NULL OR a.id IS NULL
),
details_multiple_origins AS (
    SELECT id::text AS example_id
    FROM detalle_pagos
    WHERE num_nonnulls(mensualidad_id, matricula_id, stock_id, concepto_id) > 1
),
details_without_origin AS (
    SELECT id::text AS example_id
    FROM detalle_pagos
    WHERE num_nonnulls(mensualidad_id, matricula_id, stock_id, concepto_id) = 0
),
monthly_fees_without_enrollment AS (
    SELECT m.id::text AS example_id
    FROM mensualidades m LEFT JOIN inscripciones i ON i.id = m.inscripcion_id
    WHERE m.inscripcion_id IS NULL OR i.id IS NULL
),
enrollments_without_owner AS (
    SELECT i.id::text AS example_id
    FROM inscripciones i
    LEFT JOIN alumnos a ON a.id = i.alumno_id
    LEFT JOIN disciplinas d ON d.id = i.disciplina_id
    WHERE i.alumno_id IS NULL OR a.id IS NULL OR i.disciplina_id IS NULL OR d.id IS NULL
),
attendance_without_owner AS (
    SELECT 'ALUMNO_MENSUAL:' || aam.id AS example_id
    FROM asistencias_alumno_mensual aam
    LEFT JOIN inscripciones i ON i.id = aam.inscripcion_id
    LEFT JOIN asistencias_mensuales am ON am.id = aam.asistencia_mensual_id
    WHERE i.id IS NULL OR am.id IS NULL
    UNION ALL
    SELECT 'DIARIA:' || ad.id
    FROM asistencias_diarias ad
    LEFT JOIN asistencias_alumno_mensual aam ON aam.id = ad.asistencia_alumno_mensual_id
    WHERE ad.asistencia_alumno_mensual_id IS NULL OR aam.id IS NULL
),
legacy_rows_without_owner AS (
    SELECT 'PAGO_MEDIO:' || pm.id AS example_id
    FROM pago_medios pm
    LEFT JOIN pagos p ON p.id = pm.pago_id
    LEFT JOIN metodo_pagos mp ON mp.id = pm.metodo_pago_id
    WHERE p.id IS NULL OR mp.id IS NULL
    UNION ALL
    SELECT 'REPORTE:' || r.id
    FROM reportes r LEFT JOIN usuarios u ON u.id = r.usuario_id
    WHERE u.id IS NULL
    UNION ALL
    SELECT 'OBS_MENSUAL:' || om.id
    FROM observaciones_mensuales om
    LEFT JOIN asistencias_mensuales am ON am.id = om.asistencia_mensual_id
    LEFT JOIN alumnos a ON a.id = om.alumno_id
    WHERE am.id IS NULL OR a.id IS NULL
    UNION ALL
    SELECT 'DISCIPLINA_DIA:' || dd.disciplina_id || ':' || dd.dia
    FROM disciplina_dias dd LEFT JOIN disciplinas d ON d.id = dd.disciplina_id
    WHERE d.id IS NULL
),
notifications_without_user AS (
    SELECT n.id::text AS example_id
    FROM notificaciones n LEFT JOIN usuarios u ON u.id = n.usuario_id
    WHERE u.id IS NULL
)
SELECT 'ORPH-PAGO-ALUMNO' AS rule_id, 'pagos' AS aggregate_name,
       COUNT(*)::bigint AS affected_count,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), '') AS sample_ids,
       'Todo pago pertenece a un alumno existente' AS violated_rule,
       'Cobro sin titular reconciliable' AS impact,
       'HUERFANO FINANCIERO' AS classification,
       'requiere decision' AS repairability
FROM payments_without_student
UNION ALL
SELECT 'ORPH-PAGO-USUARIO', 'pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'El historial de cobro debe conservar actor cuando la operacion lo requiera',
       'Auditoria incompleta; V060 permite usuario_id nulo', 'AUDITORIA', 'requiere decision'
FROM payments_without_user
UNION ALL
SELECT 'ORPH-DETALLE-PAGO', 'detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Todo detalle financiero debe referenciar su cabecera',
       'Linea sin transaccion; V060 permite que pago_id quede nulo al perder la cabecera', 'HUERFANO FINANCIERO', 'requiere decision'
FROM details_without_payment
UNION ALL
SELECT 'ORPH-DETALLE-ALUMNO', 'detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Todo detalle pertenece a un alumno existente',
       'Linea sin titular', 'HUERFANO FINANCIERO', 'requiere decision'
FROM details_without_student
UNION ALL
SELECT 'ORPH-DETALLE-MULTIPLES-ORIGENES', 'detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Un detalle referencia exactamente un origen',
       'No se puede determinar que cargo fue cobrado', 'ORIGEN AMBIGUO', 'requiere decision'
FROM details_multiple_origins
UNION ALL
SELECT 'ORPH-DETALLE-SIN-ORIGEN', 'detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Un detalle referencia exactamente un origen',
       'No existe cargo reconciliable', 'ORIGEN AUSENTE', 'no reparable automaticamente'
FROM details_without_origin
UNION ALL
SELECT 'ORPH-MENSUALIDAD-INSCRIPCION', 'mensualidades', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Toda mensualidad pertenece a una inscripcion existente',
       'Deuda sin contrato academico', 'HUERFANO FINANCIERO', 'requiere decision'
FROM monthly_fees_without_enrollment
UNION ALL
SELECT 'ORPH-INSCRIPCION-PROPIETARIO', 'inscripciones', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Toda inscripcion referencia alumno y disciplina existentes',
       'Relacion academica incompleta', 'HUERFANO ACADEMICO', 'requiere decision'
FROM enrollments_without_owner
UNION ALL
SELECT 'ORPH-ASISTENCIA-PROPIETARIO', 'asistencias', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Asistencia mensual/diaria referencia inscripcion y planilla existentes',
       'Historial de asistencia no atribuible', 'HUERFANO ACADEMICO', 'requiere decision'
FROM attendance_without_owner
UNION ALL
SELECT 'ORPH-FILA-HEREDADA-PROPIETARIO', 'tablas_heredadas', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Filas heredadas conservan propietario o referencia existente',
       'V060 conserva tablas sin entidad y algunas sin FK vigente', 'HUERFANO HEREDADO', 'requiere decision'
FROM legacy_rows_without_owner
UNION ALL
SELECT 'ORPH-NOTIFICACION-USUARIO', 'notificaciones', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Toda notificacion referencia un usuario existente',
       'Destinatario inexistente; V060 no tiene FK', 'HUERFANO OPERATIVO', 'inequivoca'
FROM notifications_without_user
ORDER BY rule_id;
