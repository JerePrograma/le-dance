-- Duplicados detectables en el catalogo real de V060. No corrige ni elige ganadores.
WITH
duplicate_enrollments AS (
    SELECT alumno_id || ':' || disciplina_id AS example_id
    FROM inscripciones
    WHERE estado = 'ACTIVA'
    GROUP BY alumno_id, disciplina_id
    HAVING COUNT(*) > 1
),
duplicate_monthly_fees AS (
    SELECT inscripcion_id || ':' || to_char(date_trunc('month', fecha_cuota), 'YYYY-MM') AS example_id
    FROM mensualidades
    GROUP BY inscripcion_id, date_trunc('month', fecha_cuota)
    HAVING COUNT(*) > 1
),
duplicate_enrollment_fees AS (
    SELECT alumno_id || ':' || anio AS example_id
    FROM matriculas
    GROUP BY alumno_id, anio
    HAVING COUNT(*) > 1
),
duplicate_monthly_attendance AS (
    SELECT disciplina_id || ':' || anio || '-' || lpad(mes::text, 2, '0') AS example_id
    FROM asistencias_mensuales
    GROUP BY disciplina_id, anio, mes
    HAVING COUNT(*) > 1
),
duplicate_daily_attendance AS (
    SELECT i.alumno_id || ':' || ad.fecha || ':' || aam.asistencia_mensual_id AS example_id
    FROM asistencias_diarias ad
    JOIN asistencias_alumno_mensual aam ON aam.id = ad.asistencia_alumno_mensual_id
    JOIN inscripciones i ON i.id = aam.inscripcion_id
    GROUP BY i.alumno_id, ad.fecha, aam.asistencia_mensual_id
    HAVING COUNT(*) > 1
),
detail_origins AS (
    SELECT id, alumno_id, 'MENSUALIDAD'::text AS origin_type, mensualidad_id AS origin_id
    FROM detalle_pagos WHERE mensualidad_id IS NOT NULL
    UNION ALL
    SELECT id, alumno_id, 'MATRICULA', matricula_id
    FROM detalle_pagos WHERE matricula_id IS NOT NULL
    UNION ALL
    SELECT id, alumno_id, 'STOCK', stock_id
    FROM detalle_pagos WHERE stock_id IS NOT NULL
    UNION ALL
    SELECT id, alumno_id, 'CONCEPTO', concepto_id
    FROM detalle_pagos WHERE concepto_id IS NOT NULL
),
duplicate_payment_details AS (
    SELECT origin_type || ':' || alumno_id || ':' || origin_id AS example_id
    FROM detail_origins
    GROUP BY origin_type, alumno_id, origin_id
    HAVING COUNT(*) > 1
),
duplicate_stock_codes AS (
    SELECT lower(btrim(codigo_barras)) AS example_id
    FROM stocks
    WHERE codigo_barras IS NOT NULL AND btrim(codigo_barras) <> ''
    GROUP BY lower(btrim(codigo_barras))
    HAVING COUNT(*) > 1
),
duplicate_users AS (
    SELECT lower(btrim(nombre_usuario)) AS example_id
    FROM usuarios
    GROUP BY lower(btrim(nombre_usuario))
    HAVING COUNT(*) > 1
),
duplicate_roles AS (
    SELECT lower(btrim(descripcion)) AS example_id
    FROM roles
    GROUP BY lower(btrim(descripcion))
    HAVING COUNT(*) > 1
),
duplicate_processes AS (
    SELECT lower(btrim(proceso)) || ':' || COALESCE(ultima_ejecucion::text, 'NULL') AS example_id
    FROM procesos_ejecutados
    GROUP BY lower(btrim(proceso)), ultima_ejecucion
    HAVING COUNT(*) > 1
)
SELECT 'DUP-INSCRIPCION-ACTIVA' AS rule_id, 'inscripciones' AS aggregate_name,
       COUNT(*)::bigint AS affected_count,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), '') AS sample_ids,
       'Una sola inscripcion ACTIVA por alumno y disciplina' AS violated_rule,
       'Puede duplicar obligaciones, asistencia y cobranza' AS impact,
       'INTEGRIDAD ACADEMICA' AS classification,
       'requiere decision' AS repairability
FROM duplicate_enrollments
UNION ALL
SELECT 'DUP-MENSUALIDAD-PERIODO', 'mensualidades', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Una mensualidad por inscripcion y periodo',
       'Puede duplicar deuda y cobro', 'INTEGRIDAD FINANCIERA', 'requiere decision'
FROM duplicate_monthly_fees
UNION ALL
SELECT 'DUP-MATRICULA-ANIO', 'matriculas', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Una matricula por alumno y anio',
       'Puede duplicar deuda anual; V060 contiene constraint unica', 'INTEGRIDAD FINANCIERA', 'inequivoca'
FROM duplicate_enrollment_fees
UNION ALL
SELECT 'DUP-ASISTENCIA-MENSUAL', 'asistencias_mensuales', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Una planilla mensual por disciplina y periodo',
       'Fragmenta o duplica asistencia', 'INTEGRIDAD ACADEMICA', 'requiere decision'
FROM duplicate_monthly_attendance
UNION ALL
SELECT 'DUP-ASISTENCIA-DIARIA', 'asistencias_diarias', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Una asistencia diaria por alumno, fecha y planilla/clase',
       'Duplica presencia o ausencia', 'INTEGRIDAD ACADEMICA', 'requiere decision'
FROM duplicate_daily_attendance
UNION ALL
SELECT 'DUP-DETALLE-ORIGEN', 'detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Un detalle autoritativo por alumno y origen salvo fraccionamiento explicitamente modelado',
       'Puede representar clones o cobro repetido', 'INTEGRIDAD FINANCIERA', 'requiere decision'
FROM duplicate_payment_details
UNION ALL
SELECT 'DUP-STOCK-CODIGO', 'stocks', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Codigo de barras no nulo normalizado identifica un stock',
       'La venta puede resolver el producto equivocado', 'INTEGRIDAD INVENTARIO', 'requiere decision'
FROM duplicate_stock_codes
UNION ALL
SELECT 'DUP-USUARIO-NORMALIZADO', 'usuarios', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Nombre de usuario normalizado unico',
       'Identidad ambigua; el unique actual distingue variantes de mayusculas/espacios', 'SEGURIDAD', 'requiere decision'
FROM duplicate_users
UNION ALL
SELECT 'DUP-ROL-NORMALIZADO', 'roles', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Descripcion de rol normalizada unica',
       'Autorizacion ambigua', 'SEGURIDAD', 'requiere decision'
FROM duplicate_roles
UNION ALL
SELECT 'DUP-PROCESO-EJECUTADO', 'procesos_ejecutados', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       'Una marca por proceso y fecha de ejecucion',
       'No garantiza idempotencia y puede ocultar doble ejecucion', 'IDEMPOTENCIA', 'requiere decision'
FROM duplicate_processes
ORDER BY rule_id;
