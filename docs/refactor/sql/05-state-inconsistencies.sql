WITH aplicado AS (
    SELECT cargo_id, sum(importe_aplicado) importe
    FROM public.aplicaciones_pago WHERE estado = 'APLICADA' GROUP BY cargo_id
), credito AS (
    SELECT coalesce(m.cargo_id, original.cargo_id) cargo_id,
           sum(CASE WHEN m.tipo = 'CONSUMO' THEN m.importe ELSE -m.importe END) importe
    FROM public.movimientos_credito m
    LEFT JOIN public.movimientos_credito original ON original.id = m.movimiento_revertido_id
    WHERE m.tipo = 'CONSUMO' OR (m.tipo = 'REVERSO' AND original.tipo = 'CONSUMO')
    GROUP BY coalesce(m.cargo_id, original.cargo_id)
), saldos AS (
    SELECT c.id, c.estado, c.importe_original,
           c.importe_original - coalesce(a.importe, 0) - coalesce(cr.importe, 0) saldo
    FROM public.cargos c LEFT JOIN aplicado a ON a.cargo_id = c.id LEFT JOIN credito cr ON cr.cargo_id = c.id
)
SELECT 'STATE-CARGO-PAGADO-CON-SALDO' AS rule_id, count(*) AS affected_count
FROM saldos WHERE estado = 'PAGADO' AND saldo <> 0
UNION ALL
SELECT 'STATE-CARGO-PENDIENTE-APLICADO', count(*)
FROM saldos WHERE estado = 'PENDIENTE' AND saldo <> importe_original
UNION ALL
SELECT 'STATE-CARGO-PARCIAL-LIMITE', count(*)
FROM saldos WHERE estado = 'PARCIAL' AND (saldo <= 0 OR saldo >= importe_original)
UNION ALL
SELECT 'STATE-PAGO-ANULADO-APLICADO', count(*)
FROM public.pagos p JOIN public.aplicaciones_pago a ON a.pago_id = p.id
WHERE p.estado = 'ANULADO' AND a.estado = 'APLICADA'
UNION ALL
SELECT 'STATE-INSCRIPCION-BAJA', count(*)
FROM public.inscripciones WHERE (estado = 'ACTIVA' AND fecha_baja IS NOT NULL)
   OR (estado <> 'ACTIVA' AND fecha_baja IS NULL)
ORDER BY rule_id;
