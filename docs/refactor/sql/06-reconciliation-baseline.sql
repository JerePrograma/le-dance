-- Baseline de reconciliacion. Conserva fuentes contradictorias en columnas separadas.
WITH
valid_details AS (
    SELECT *
    FROM detalle_pagos
    WHERE estado_pago <> 'ANULADO' AND COALESCE(removido, false) = false
),
details_by_payment AS (
    SELECT pago_id, COUNT(*)::bigint AS detail_count, COALESCE(SUM(a_cobrar), 0) AS detail_total
    FROM valid_details
    WHERE pago_id IS NOT NULL
    GROUP BY pago_id
),
payments_enriched AS (
    SELECT p.*,
           COALESCE(d.detail_count, 0)::bigint AS detail_count,
           COALESCE(d.detail_total, 0)::numeric AS detail_total
    FROM pagos p LEFT JOIN details_by_payment d ON d.pago_id = p.id
),
by_student AS (
    SELECT p.alumno_id,
           SUM(p.importe_inicial) AS original_amount,
           SUM(p.monto_pagado) AS collected_amount,
           SUM(p.saldo_restante) AS balance_amount,
           SUM(p.detail_count)::bigint AS detail_count,
           SUM(p.detail_total) AS detail_total,
           MAX(a.credito_acumulado)::numeric AS credit_amount
    FROM payments_enriched p
    JOIN alumnos a ON a.id = p.alumno_id
    GROUP BY p.alumno_id
),
payments_by_date AS (
    SELECT fecha,
           SUM(importe_inicial) AS original_amount,
           SUM(monto_pagado) AS collected_amount,
           SUM(saldo_restante) AS balance_amount,
           SUM(detail_count)::bigint AS detail_count,
           SUM(detail_total) AS detail_total
    FROM payments_enriched
    WHERE estado_pago <> 'ANULADO'
    GROUP BY fecha
),
expenses_by_date AS (
    SELECT fecha, SUM(monto) AS expense_amount FROM egresos WHERE activo GROUP BY fecha
),
cash_by_date AS (
    SELECT fecha, SUM(total_efectivo + total_transferencia + total_tarjeta) AS cash_amount
    FROM caja WHERE activo GROUP BY fecha
),
date_keys AS (
    SELECT fecha FROM payments_by_date
    UNION SELECT fecha FROM expenses_by_date
    UNION SELECT fecha FROM cash_by_date
),
monthly_by_period AS (
    SELECT date_trunc('month', fecha_cuota)::date AS period,
           SUM(COALESCE(importe_inicial, 0))::numeric AS original_amount,
           SUM(monto_abonado) AS monthly_collected,
           SUM(COALESCE(importe_pendiente, 0))::numeric AS monthly_balance
    FROM mensualidades
    GROUP BY date_trunc('month', fecha_cuota)::date
),
payments_by_period AS (
    SELECT date_trunc('month', fecha)::date AS period,
           SUM(monto_pagado) AS payment_collected,
           SUM(detail_count)::bigint AS detail_count,
           SUM(detail_total) AS detail_total
    FROM payments_enriched
    WHERE estado_pago <> 'ANULADO'
    GROUP BY date_trunc('month', fecha)::date
),
period_keys AS (
    SELECT period FROM monthly_by_period
    UNION SELECT period FROM payments_by_period
),
details_by_monthly_fee AS (
    SELECT mensualidad_id, COUNT(*)::bigint AS detail_count, COALESCE(SUM(a_cobrar), 0) AS detail_total
    FROM valid_details
    WHERE mensualidad_id IS NOT NULL
    GROUP BY mensualidad_id
),
payments_by_method AS (
    SELECT metodo_pago_id,
           SUM(importe_inicial) AS original_amount,
           SUM(monto_pagado) AS collected_amount,
           SUM(saldo_restante) AS balance_amount,
           SUM(detail_count)::bigint AS detail_count,
           SUM(detail_total) AS detail_total
    FROM payments_enriched
    WHERE estado_pago <> 'ANULADO'
    GROUP BY metodo_pago_id
),
expenses_by_method AS (
    SELECT metodo_pago_id, SUM(monto) AS expense_amount
    FROM egresos WHERE activo GROUP BY metodo_pago_id
),
legacy_cash_by_method AS (
    SELECT 'efectivo'::text AS normalized_method, SUM(total_efectivo) AS cash_amount FROM caja WHERE activo
    UNION ALL SELECT 'transferencia', SUM(total_transferencia) FROM caja WHERE activo
    UNION ALL SELECT 'tarjeta', SUM(total_tarjeta) FROM caja WHERE activo
)
SELECT 'ALUMNO'::text AS dimension,
       s.alumno_id::text AS dimension_key,
       s.alumno_id::bigint AS alumno_id,
       NULL::date AS fecha,
       NULL::date AS periodo,
       NULL::bigint AS pago_id,
       NULL::bigint AS mensualidad_id,
       NULL::bigint AS metodo_pago_id,
       s.original_amount::numeric AS importe_original,
       s.collected_amount::numeric AS monto_cobrado,
       s.balance_amount::numeric AS saldo,
       s.detail_count::bigint AS detalle_count,
       s.detail_total::numeric AS detalles,
       s.credit_amount::numeric AS credito,
       NULL::numeric AS egresos,
       NULL::numeric AS caja,
       (s.collected_amount + s.balance_amount - s.original_amount)::numeric AS diferencia_balance,
       (s.detail_total - s.collected_amount)::numeric AS diferencia_detalles,
       NULL::numeric AS diferencia_caja,
       'credito_acumulado no tiene movimientos de respaldo en V060'::text AS notes
FROM by_student s
UNION ALL
SELECT 'FECHA', k.fecha::text, NULL::bigint, k.fecha, NULL::date, NULL::bigint, NULL::bigint, NULL::bigint,
       COALESCE(p.original_amount, 0), COALESCE(p.collected_amount, 0), COALESCE(p.balance_amount, 0),
       COALESCE(p.detail_count, 0), COALESCE(p.detail_total, 0), NULL::numeric,
       COALESCE(e.expense_amount, 0), COALESCE(c.cash_amount, 0),
       COALESCE(p.collected_amount, 0) + COALESCE(p.balance_amount, 0) - COALESCE(p.original_amount, 0),
       COALESCE(p.detail_total, 0) - COALESCE(p.collected_amount, 0),
       COALESCE(c.cash_amount, 0) - COALESCE(p.collected_amount, 0) + COALESCE(e.expense_amount, 0),
       'caja, pagos y egresos heredados se muestran como fuentes independientes'
FROM date_keys k
LEFT JOIN payments_by_date p ON p.fecha = k.fecha
LEFT JOIN expenses_by_date e ON e.fecha = k.fecha
LEFT JOIN cash_by_date c ON c.fecha = k.fecha
UNION ALL
SELECT 'PERIODO', k.period::text, NULL::bigint, NULL::date, k.period, NULL::bigint, NULL::bigint, NULL::bigint,
       COALESCE(m.original_amount, 0), COALESCE(p.payment_collected, 0), COALESCE(m.monthly_balance, 0),
       COALESCE(p.detail_count, 0), COALESCE(p.detail_total, 0), NULL::numeric, NULL::numeric, NULL::numeric,
       COALESCE(m.monthly_collected, 0) + COALESCE(m.monthly_balance, 0) - COALESCE(m.original_amount, 0),
       COALESCE(p.detail_total, 0) - COALESCE(p.payment_collected, 0), NULL::numeric,
       'monto_cobrado proviene de pagos por fecha; balance e importe original provienen de mensualidades por fecha_cuota'
FROM period_keys k
LEFT JOIN monthly_by_period m ON m.period = k.period
LEFT JOIN payments_by_period p ON p.period = k.period
UNION ALL
SELECT 'PAGO', p.id::text, p.alumno_id, p.fecha, date_trunc('month', p.fecha)::date, p.id, NULL::bigint, p.metodo_pago_id,
       p.importe_inicial, p.monto_pagado, p.saldo_restante, p.detail_count, p.detail_total,
       NULL::numeric, NULL::numeric, NULL::numeric,
       p.monto_pagado + p.saldo_restante - p.importe_inicial,
       p.detail_total - p.monto_pagado, NULL::numeric,
       'monto de cabecera=' || p.monto || '; estado=' || p.estado_pago
FROM payments_enriched p
UNION ALL
SELECT 'MENSUALIDAD', m.id::text, i.alumno_id, m.fecha_pago, date_trunc('month', m.fecha_cuota)::date,
       NULL::bigint, m.id, NULL::bigint,
       COALESCE(m.importe_inicial, 0)::numeric, m.monto_abonado, COALESCE(m.importe_pendiente, 0)::numeric,
       COALESCE(d.detail_count, 0), COALESCE(d.detail_total, 0), NULL::numeric, NULL::numeric, NULL::numeric,
       m.monto_abonado + COALESCE(m.importe_pendiente, 0) - COALESCE(m.importe_inicial, 0),
       COALESCE(d.detail_total, 0) - m.monto_abonado, NULL::numeric,
       'estado=' || m.estado || '; fecha_cuota=' || m.fecha_cuota
FROM mensualidades m
JOIN inscripciones i ON i.id = m.inscripcion_id
LEFT JOIN details_by_monthly_fee d ON d.mensualidad_id = m.id
UNION ALL
SELECT 'METODO_PAGO', COALESCE(mp.id::text, 'NULL'), NULL::bigint, NULL::date, NULL::date,
       NULL::bigint, NULL::bigint, mp.id,
       COALESCE(p.original_amount, 0), COALESCE(p.collected_amount, 0), COALESCE(p.balance_amount, 0),
       COALESCE(p.detail_count, 0), COALESCE(p.detail_total, 0), NULL::numeric,
       COALESCE(e.expense_amount, 0), c.cash_amount,
       COALESCE(p.collected_amount, 0) + COALESCE(p.balance_amount, 0) - COALESCE(p.original_amount, 0),
       COALESCE(p.detail_total, 0) - COALESCE(p.collected_amount, 0),
       CASE WHEN c.cash_amount IS NULL THEN NULL
            ELSE c.cash_amount - COALESCE(p.collected_amount, 0) + COALESCE(e.expense_amount, 0) END,
       'metodo=' || mp.descripcion || CASE WHEN c.cash_amount IS NULL THEN '; sin columna de caja asociable inequivocamente' ELSE '' END
FROM metodo_pagos mp
LEFT JOIN payments_by_method p ON p.metodo_pago_id = mp.id
LEFT JOIN expenses_by_method e ON e.metodo_pago_id = mp.id
LEFT JOIN legacy_cash_by_method c ON c.normalized_method = lower(btrim(mp.descripcion))
ORDER BY dimension, dimension_key;
