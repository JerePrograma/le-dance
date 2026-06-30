SELECT 'cargos_originales' AS metric, coalesce(sum(importe_original), 0)::numeric(19,2) AS value FROM public.cargos
UNION ALL
SELECT 'pagos_registrados', coalesce(sum(monto_recibido), 0)::numeric(19,2) FROM public.pagos WHERE estado = 'REGISTRADO'
UNION ALL
SELECT 'aplicaciones_activas', coalesce(sum(importe_aplicado), 0)::numeric(19,2)
FROM public.aplicaciones_pago WHERE estado = 'APLICADA'
UNION ALL
SELECT 'caja_neta', coalesce(sum(CASE WHEN tipo IN ('INGRESO_PAGO','AJUSTE') THEN importe ELSE -importe END), 0)::numeric(19,2)
FROM public.movimientos_caja
UNION ALL
SELECT 'credito_neto', coalesce(sum(CASE
    WHEN m.tipo IN ('GENERACION','AJUSTE_CREDITO') THEN m.importe
    WHEN m.tipo IN ('CONSUMO','AJUSTE_DEBITO') THEN -m.importe
    WHEN original.tipo IN ('GENERACION','AJUSTE_CREDITO') THEN -m.importe ELSE m.importe END), 0)::numeric(19,2)
FROM public.movimientos_credito m LEFT JOIN public.movimientos_credito original ON original.id = m.movimiento_revertido_id
ORDER BY metric;
