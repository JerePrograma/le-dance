-- Inventario V060 de solo lectura. Una fila por tabla o metrica de esquema.
SELECT 'COUNT-ALUMNOS' AS metric_id, 'alumnos' AS table_name,
       COUNT(*)::bigint AS row_count,
       COUNT(*) FILTER (WHERE activo)::bigint AS active_count,
       COUNT(*) FILTER (WHERE NOT activo)::bigint AS inactive_count,
       MIN(id)::bigint AS min_id, MAX(id)::bigint AS max_id,
       MIN(fecha_incorporacion) AS min_date, MAX(fecha_incorporacion) AS max_date,
       COUNT(*) FILTER (WHERE credito_acumulado IS NULL)::bigint AS relevant_null_count,
       'nulls=credito_acumulado; rango=fecha_incorporacion'::text AS detail
FROM alumnos
UNION ALL
SELECT 'COUNT-ASISTENCIAS-ALUMNO-MENSUAL', 'asistencias_alumno_mensual', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE inscripcion_id IS NULL OR asistencia_mensual_id IS NULL)::bigint,
       'nulls=inscripcion_id|asistencia_mensual_id'
FROM asistencias_alumno_mensual
UNION ALL
SELECT 'COUNT-ASISTENCIAS-DIARIAS', 'asistencias_diarias', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, MIN(fecha), MAX(fecha),
       COUNT(*) FILTER (WHERE asistencia_alumno_mensual_id IS NULL)::bigint,
       'nulls=asistencia_alumno_mensual_id; rango=fecha'
FROM asistencias_diarias
UNION ALL
SELECT 'COUNT-ASISTENCIAS-MENSUALES', 'asistencias_mensuales', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint,
       MIN(make_date(anio, mes, 1)), MAX(make_date(anio, mes, 1)),
       COUNT(*) FILTER (WHERE disciplina_id IS NULL)::bigint,
       'nulls=disciplina_id; rango=mes/anio'
FROM asistencias_mensuales
UNION ALL
SELECT 'COUNT-BONIFICACIONES', 'bonificaciones', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE valor_fijo IS NULL)::bigint, 'nulls=valor_fijo'
FROM bonificaciones
UNION ALL
SELECT 'COUNT-CAJA', 'caja', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, MIN(fecha), MAX(fecha), 0::bigint, 'rango=fecha'
FROM caja
UNION ALL
SELECT 'COUNT-CONCEPTOS', 'conceptos', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE sub_concepto_id IS NULL)::bigint, 'nulls=sub_concepto_id'
FROM conceptos
UNION ALL
SELECT 'COUNT-DETALLE-PAGOS', 'detalle_pagos', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE estado_pago = 'ACTIVO' AND COALESCE(removido, false) = false)::bigint,
       COUNT(*) FILTER (WHERE estado_pago <> 'ACTIVO' OR COALESCE(removido, false))::bigint,
       MIN(id)::bigint, MAX(id)::bigint, MIN(fecha_registro), MAX(fecha_registro),
       COUNT(*) FILTER (WHERE pago_id IS NULL OR usuario_id IS NULL)::bigint,
       'nulls=pago_id|usuario_id; rango=fecha_registro'
FROM detalle_pagos
UNION ALL
SELECT 'COUNT-DISCIPLINA-DIAS', 'disciplina_dias', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(disciplina_id)::bigint, MAX(disciplina_id)::bigint,
       NULL::date, NULL::date, 0::bigint, 'id=disciplina_id'
FROM disciplina_dias
UNION ALL
SELECT 'COUNT-DISCIPLINA-HORARIOS', 'disciplina_horarios', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE disciplina_id IS NULL)::bigint, 'nulls=disciplina_id'
FROM disciplina_horarios
UNION ALL
SELECT 'COUNT-DISCIPLINAS', 'disciplinas', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE profesor_id IS NULL OR salon_id IS NULL)::bigint,
       'nulls=profesor_id|salon_id'
FROM disciplinas
UNION ALL
SELECT 'COUNT-EGRESOS', 'egresos', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, MIN(fecha), MAX(fecha),
       COUNT(*) FILTER (WHERE metodo_pago_id IS NULL)::bigint, 'nulls=metodo_pago_id; rango=fecha'
FROM egresos
UNION ALL
SELECT 'COUNT-INSCRIPCIONES', 'inscripciones', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE estado = 'ACTIVA')::bigint,
       COUNT(*) FILTER (WHERE estado <> 'ACTIVA')::bigint,
       MIN(id)::bigint, MAX(id)::bigint, MIN(fecha_inscripcion), MAX(fecha_inscripcion),
       COUNT(*) FILTER (WHERE alumno_id IS NULL OR disciplina_id IS NULL)::bigint,
       'nulls=alumno_id|disciplina_id; rango=fecha_inscripcion'
FROM inscripciones
UNION ALL
SELECT 'COUNT-MATRICULAS', 'matriculas', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE NOT pagada)::bigint, COUNT(*) FILTER (WHERE pagada)::bigint,
       MIN(id)::bigint, MAX(id)::bigint,
       MIN(make_date(anio, 1, 1)), MAX(make_date(anio, 1, 1)),
       COUNT(*) FILTER (WHERE alumno_id IS NULL)::bigint, 'nulls=alumno_id; rango=anio'
FROM matriculas
UNION ALL
SELECT 'COUNT-MENSUALIDADES', 'mensualidades', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE estado <> 'PAGADO')::bigint,
       COUNT(*) FILTER (WHERE estado = 'PAGADO')::bigint,
       MIN(id)::bigint, MAX(id)::bigint, MIN(fecha_cuota), MAX(fecha_cuota),
       COUNT(*) FILTER (WHERE inscripcion_id IS NULL OR importe_inicial IS NULL OR importe_pendiente IS NULL)::bigint,
       'nulls=inscripcion_id|importe_inicial|importe_pendiente; rango=fecha_cuota'
FROM mensualidades
UNION ALL
SELECT 'COUNT-METODO-PAGOS', 'metodo_pagos', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date, 0::bigint, 'catalogo'
FROM metodo_pagos
UNION ALL
SELECT 'COUNT-NOTIFICACIONES', 'notificaciones', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE NOT leida)::bigint, COUNT(*) FILTER (WHERE leida)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, MIN(fecha_creacion)::date, MAX(fecha_creacion)::date,
       COUNT(*) FILTER (WHERE usuario_id IS NULL)::bigint, 'nulls=usuario_id; rango=fecha_creacion'
FROM notificaciones
UNION ALL
SELECT 'COUNT-OBSERVACIONES-MENSUALES', 'observaciones_mensuales', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE asistencia_mensual_id IS NULL OR alumno_id IS NULL)::bigint,
       'nulls=asistencia_mensual_id|alumno_id'
FROM observaciones_mensuales
UNION ALL
SELECT 'COUNT-OBSERVACIONES-PROFESORES', 'observaciones_profesores', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, MIN(fecha), MAX(fecha),
       COUNT(*) FILTER (WHERE profesor_id IS NULL)::bigint, 'nulls=profesor_id; rango=fecha'
FROM observaciones_profesores
UNION ALL
SELECT 'COUNT-PAGO-MEDIOS', 'pago_medios', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE pago_id IS NULL OR metodo_pago_id IS NULL)::bigint,
       'nulls=pago_id|metodo_pago_id'
FROM pago_medios
UNION ALL
SELECT 'COUNT-PAGOS', 'pagos', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE estado_pago = 'ACTIVO')::bigint,
       COUNT(*) FILTER (WHERE estado_pago <> 'ACTIVO')::bigint,
       MIN(id)::bigint, MAX(id)::bigint, MIN(fecha), MAX(fecha),
       COUNT(*) FILTER (WHERE usuario_id IS NULL OR metodo_pago_id IS NULL)::bigint,
       'nulls=usuario_id|metodo_pago_id; rango=fecha'
FROM pagos
UNION ALL
SELECT 'COUNT-PROCESOS-EJECUTADOS', 'procesos_ejecutados', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint,
       MIN(ultima_ejecucion), MAX(ultima_ejecucion),
       COUNT(*) FILTER (WHERE ultima_ejecucion IS NULL)::bigint,
       'nulls=ultima_ejecucion; rango=ultima_ejecucion'
FROM procesos_ejecutados
UNION ALL
SELECT 'COUNT-PROFESORES', 'profesores', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE usuario_id IS NULL)::bigint, 'nulls=usuario_id'
FROM profesores
UNION ALL
SELECT 'COUNT-RECARGOS', 'recargos', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE valor_fijo IS NULL)::bigint, 'nulls=valor_fijo'
FROM recargos
UNION ALL
SELECT 'COUNT-REPORTES', 'reportes', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, MIN(fecha_generacion), MAX(fecha_generacion),
       COUNT(*) FILTER (WHERE usuario_id IS NULL)::bigint, 'nulls=usuario_id; rango=fecha_generacion'
FROM reportes
UNION ALL
SELECT 'COUNT-ROLES', 'roles', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date, 0::bigint, 'catalogo'
FROM roles
UNION ALL
SELECT 'COUNT-SALONES', 'salones', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE descripcion IS NULL)::bigint, 'nulls=descripcion'
FROM salones
UNION ALL
SELECT 'COUNT-STOCKS', 'stocks', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, MIN(fecha_ingreso), MAX(fecha_ingreso),
       COUNT(*) FILTER (WHERE codigo_barras IS NULL)::bigint, 'nulls=codigo_barras; rango=fecha_ingreso'
FROM stocks
UNION ALL
SELECT 'COUNT-SUB-CONCEPTOS', 'sub_conceptos', COUNT(*)::bigint,
       NULL::bigint, NULL::bigint, MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       0::bigint, 'catalogo'
FROM sub_conceptos
UNION ALL
SELECT 'COUNT-USUARIOS', 'usuarios', COUNT(*)::bigint,
       COUNT(*) FILTER (WHERE activo)::bigint, COUNT(*) FILTER (WHERE NOT activo)::bigint,
       MIN(id)::bigint, MAX(id)::bigint, NULL::date, NULL::date,
       COUNT(*) FILTER (WHERE rol_id IS NULL)::bigint, 'nulls=rol_id'
FROM usuarios
UNION ALL
SELECT 'FLYWAY-VERSION', 'flyway_schema_history', COUNT(*) FILTER (WHERE success)::bigint,
       NULL::bigint, NULL::bigint, NULL::bigint, NULL::bigint,
       MIN(installed_on)::date, MAX(installed_on)::date,
       COUNT(*) FILTER (WHERE checksum IS NULL)::bigint,
       'version=' || COALESCE((SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1), 'NINGUNA')
FROM flyway_schema_history
ORDER BY metric_id;
