SELECT 'DUP-USUARIO-NORMALIZADO' AS rule_id, count(*) AS affected_count
FROM (SELECT lower(nombre_usuario) FROM public.usuarios GROUP BY lower(nombre_usuario) HAVING count(*) > 1) d
UNION ALL
SELECT 'DUP-INSCRIPCION-ACTIVA', count(*)
FROM (SELECT alumno_id, disciplina_id FROM public.inscripciones WHERE estado = 'ACTIVA'
      GROUP BY alumno_id, disciplina_id HAVING count(*) > 1) d
UNION ALL
SELECT 'DUP-MENSUALIDAD-PERIODO', count(*)
FROM (SELECT inscripcion_id, anio, mes FROM public.mensualidades
      GROUP BY inscripcion_id, anio, mes HAVING count(*) > 1) d
UNION ALL
SELECT 'DUP-MATRICULA-PERIODO', count(*)
FROM (SELECT alumno_id, anio FROM public.matriculas GROUP BY alumno_id, anio HAVING count(*) > 1) d
ORDER BY rule_id;
