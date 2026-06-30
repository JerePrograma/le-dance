SELECT 'ORPH-CARGO-ALUMNO' AS rule_id, count(*) AS affected_count
FROM public.cargos c LEFT JOIN public.alumnos a ON a.id = c.alumno_id WHERE a.id IS NULL
UNION ALL
SELECT 'ORPH-APLICACION-PAGO', count(*)
FROM public.aplicaciones_pago a LEFT JOIN public.pagos p ON p.id = a.pago_id WHERE p.id IS NULL
UNION ALL
SELECT 'ORPH-APLICACION-CARGO', count(*)
FROM public.aplicaciones_pago a LEFT JOIN public.cargos c ON c.id = a.cargo_id WHERE c.id IS NULL
UNION ALL
SELECT 'ORPH-MOVIMIENTO-CAJA-ORIGEN', count(*)
FROM public.movimientos_caja m
WHERE (m.tipo = 'INGRESO_PAGO' AND m.pago_id IS NULL)
   OR (m.tipo = 'EGRESO' AND m.egreso_id IS NULL)
   OR (m.tipo = 'REVERSO' AND m.movimiento_revertido_id IS NULL)
UNION ALL
SELECT 'ORPH-MOVIMIENTO-CREDITO-ORIGEN', count(*)
FROM public.movimientos_credito m
WHERE (m.tipo = 'GENERACION' AND m.pago_id IS NULL)
   OR (m.tipo = 'CONSUMO' AND m.cargo_id IS NULL)
   OR (m.tipo = 'REVERSO' AND m.movimiento_revertido_id IS NULL)
ORDER BY rule_id;
