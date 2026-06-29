-- Comparaciones exactas: no aplica epsilon ni tolerancias flotantes.
WITH
negative_amounts AS (
    SELECT 'PAGO.monto' AS source, id, monto::numeric AS value FROM pagos WHERE monto < 0
    UNION ALL SELECT 'PAGO.importe_inicial', id, importe_inicial FROM pagos WHERE importe_inicial < 0
    UNION ALL SELECT 'PAGO.saldo_restante', id, saldo_restante FROM pagos WHERE saldo_restante < 0
    UNION ALL SELECT 'PAGO.monto_pagado', id, monto_pagado FROM pagos WHERE monto_pagado < 0
    UNION ALL SELECT 'PAGO.valor_base', id, valor_base::numeric FROM pagos WHERE valor_base < 0
    UNION ALL SELECT 'DETALLE.valor_base', id, valor_base FROM detalle_pagos WHERE valor_base < 0
    UNION ALL SELECT 'DETALLE.importe_inicial', id, importe_inicial FROM detalle_pagos WHERE importe_inicial < 0
    UNION ALL SELECT 'DETALLE.importe_pendiente', id, importe_pendiente FROM detalle_pagos WHERE importe_pendiente < 0
    UNION ALL SELECT 'DETALLE.a_cobrar', id, a_cobrar FROM detalle_pagos WHERE a_cobrar < 0
    UNION ALL SELECT 'MENSUALIDAD.valor_base', id, valor_base FROM mensualidades WHERE valor_base < 0
    UNION ALL SELECT 'MENSUALIDAD.importe_inicial', id, importe_inicial::numeric FROM mensualidades WHERE importe_inicial < 0
    UNION ALL SELECT 'MENSUALIDAD.importe_pendiente', id, importe_pendiente::numeric FROM mensualidades WHERE importe_pendiente < 0
    UNION ALL SELECT 'MENSUALIDAD.monto_abonado', id, monto_abonado FROM mensualidades WHERE monto_abonado < 0
    UNION ALL SELECT 'ALUMNO.cuota_total', id, cuota_total FROM alumnos WHERE cuota_total < 0
    UNION ALL SELECT 'ALUMNO.credito_acumulado', id, credito_acumulado::numeric FROM alumnos WHERE credito_acumulado < 0
    UNION ALL SELECT 'STOCK.precio', id, precio FROM stocks WHERE precio < 0
    UNION ALL SELECT 'STOCK.stock', id, stock::numeric FROM stocks WHERE stock < 0
    UNION ALL SELECT 'EGRESO.monto', id, monto FROM egresos WHERE monto < 0
    UNION ALL SELECT 'CAJA.total_efectivo', id, total_efectivo FROM caja WHERE total_efectivo < 0
    UNION ALL SELECT 'CAJA.total_transferencia', id, total_transferencia FROM caja WHERE total_transferencia < 0
    UNION ALL SELECT 'CAJA.total_tarjeta', id, total_tarjeta FROM caja WHERE total_tarjeta < 0
    UNION ALL SELECT 'CONCEPTO.precio', id, precio FROM conceptos WHERE precio < 0
    UNION ALL SELECT 'DISCIPLINA.valor_cuota', id, valor_cuota FROM disciplinas WHERE valor_cuota < 0
    UNION ALL SELECT 'DISCIPLINA.clase_suelta', id, clase_suelta FROM disciplinas WHERE clase_suelta < 0
    UNION ALL SELECT 'DISCIPLINA.clase_prueba', id, clase_prueba FROM disciplinas WHERE clase_prueba < 0
),
payment_initial_mismatch AS (
    SELECT id::text AS example_id,
           'id=' || id || ',valor_base=' || valor_base || ',importe_inicial=' || importe_inicial ||
           ',diferencia=' || (importe_inicial - valor_base) AS observed_value
    FROM pagos
    WHERE valor_base IS NOT NULL AND importe_inicial <> valor_base
),
payment_balance_mismatch AS (
    SELECT id::text AS example_id,
           'id=' || id || ',pagado=' || monto_pagado || ',saldo=' || saldo_restante ||
           ',inicial=' || importe_inicial || ',diferencia=' || ((monto_pagado + saldo_restante) - importe_inicial) AS observed_value
    FROM pagos
    WHERE monto_pagado + saldo_restante <> importe_inicial
),
detail_over_pending AS (
    SELECT id::text AS example_id,
           'id=' || id || ',a_cobrar=' || a_cobrar || ',pendiente=' || importe_pendiente ||
           ',diferencia=' || (a_cobrar - importe_pendiente) AS observed_value
    FROM detalle_pagos
    WHERE a_cobrar IS NOT NULL AND importe_pendiente IS NOT NULL AND a_cobrar > importe_pendiente
),
payment_detail_totals AS (
    SELECT p.id,
           p.monto,
           COALESCE(SUM(d.a_cobrar) FILTER (
               WHERE COALESCE(d.removido, false) = false AND d.estado_pago <> 'ANULADO'), 0) AS detail_total
    FROM pagos p
    LEFT JOIN detalle_pagos d ON d.pago_id = p.id
    GROUP BY p.id, p.monto
),
payment_detail_mismatch AS (
    SELECT id::text AS example_id,
           'id=' || id || ',cabecera=' || monto || ',detalles=' || detail_total ||
           ',diferencia=' || (detail_total - monto) AS observed_value
    FROM payment_detail_totals
    WHERE detail_total <> monto
),
monthly_balance_mismatch AS (
    SELECT id::text AS example_id,
           'id=' || id || ',abonado=' || monto_abonado || ',pendiente=' || importe_pendiente ||
           ',inicial=' || importe_inicial || ',diferencia=' || ((monto_abonado + importe_pendiente) - importe_inicial) AS observed_value
    FROM mensualidades
    WHERE importe_inicial IS NOT NULL AND importe_pendiente IS NOT NULL
      AND monto_abonado + importe_pendiente <> importe_inicial
),
clamped_balances AS (
    SELECT 'PAGO:' || id AS example_id,
           'pagado=' || monto_pagado || ',inicial=' || importe_inicial || ',saldo_guardado=' || saldo_restante AS observed_value
    FROM pagos
    WHERE saldo_restante = 0 AND monto_pagado > importe_inicial
    UNION ALL
    SELECT 'MENSUALIDAD:' || id,
           'abonado=' || monto_abonado || ',inicial=' || importe_inicial || ',pendiente_guardado=' || importe_pendiente
    FROM mensualidades
    WHERE importe_pendiente = 0 AND importe_inicial IS NOT NULL AND monto_abonado > importe_inicial
),
unrepresented_overpayments AS (
    SELECT 'PAGO:' || id AS example_id,
           'exceso=' || (monto_pagado - importe_inicial) AS observed_value
    FROM pagos WHERE monto_pagado > importe_inicial
    UNION ALL
    SELECT 'MENSUALIDAD:' || id,
           'exceso=' || (monto_abonado - importe_inicial)
    FROM mensualidades
    WHERE importe_inicial IS NOT NULL AND monto_abonado > importe_inicial
),
credits_without_ledger AS (
    SELECT id::text AS example_id,
           'alumno_id=' || id || ',credito_acumulado=' || credito_acumulado AS observed_value
    FROM alumnos
    WHERE COALESCE(credito_acumulado, 0) <> 0
),
possible_payment_clones AS (
    SELECT MIN(id)::text AS example_id,
           'alumno=' || alumno_id || ',fecha=' || fecha || ',cantidad=' || COUNT(*) ||
           ',monto=' || monto || ',inicial=' || importe_inicial || ',saldo=' || saldo_restante AS observed_value
    FROM pagos
    GROUP BY alumno_id, fecha, fecha_vencimiento, monto, importe_inicial, saldo_restante, estado_pago, metodo_pago_id
    HAVING COUNT(*) > 1
),
cloned_details AS (
    SELECT id::text AS example_id,
           'id=' || id || ',pago=' || COALESCE(pago_id::text, 'NULL') || ',tipo=' || tipo AS observed_value
    FROM detalle_pagos
    WHERE COALESCE(es_clon, false)
),
partial_payment_copies AS (
    SELECT 'MENSUALIDAD:' || id AS example_id,
           'mensualidad_clon=true' AS observed_value
    FROM mensualidades WHERE COALESCE(es_clon, false)
    UNION ALL
    SELECT 'DETALLE:' || id, 'detalle_clon=true'
    FROM detalle_pagos WHERE COALESCE(es_clon, false)
),
cash_by_date AS (
    SELECT fecha, SUM(total_efectivo + total_transferencia + total_tarjeta) AS cash_total
    FROM caja WHERE activo GROUP BY fecha
),
payments_by_date AS (
    SELECT fecha, SUM(monto_pagado) AS payment_total
    FROM pagos WHERE estado_pago <> 'ANULADO' GROUP BY fecha
),
expenses_by_date AS (
    SELECT fecha, SUM(monto) AS expense_total
    FROM egresos WHERE activo GROUP BY fecha
),
cash_mismatch AS (
    SELECT COALESCE(c.fecha, p.fecha, e.fecha) AS business_date,
           COALESCE(c.cash_total, 0) AS cash_total,
           COALESCE(p.payment_total, 0) AS payment_total,
           COALESCE(e.expense_total, 0) AS expense_total
    FROM cash_by_date c
    FULL JOIN payments_by_date p ON p.fecha = c.fecha
    FULL JOIN expenses_by_date e ON e.fecha = COALESCE(c.fecha, p.fecha)
    WHERE COALESCE(c.cash_total, 0) <> COALESCE(p.payment_total, 0) - COALESCE(e.expense_total, 0)
),
charged_stock_without_movement AS (
    SELECT d.id::text AS example_id,
           'detalle=' || d.id || ',stock=' || d.stock_id || ',cobrado=' || d.a_cobrar AS observed_value
    FROM detalle_pagos d
    JOIN stocks s ON s.id = d.stock_id
    WHERE d.stock_id IS NOT NULL AND d.cobrado
      AND d.estado_pago <> 'ANULADO' AND COALESCE(d.removido, false) = false
      AND COALESCE(s.requiere_control_de_stock, false)
)
SELECT 'FIN-IMPORTE-NEGATIVO' AS rule_id, 'multiples_tablas' AS aggregate_name,
       COUNT(*)::bigint AS affected_count,
       COALESCE(array_to_string((array_agg(source || ':' || id ORDER BY source, id))[1:10], ', '), '') AS sample_ids,
       COALESCE(array_to_string((array_agg(source || ':' || id || '=' || value ORDER BY source, id))[1:10], ', '), '') AS observed_values,
       'Los importes y saldos auditados no deben ser negativos' AS violated_rule,
       'Deuda, cobro, caja, credito o stock materialmente incorrecto' AS impact,
       'INTEGRIDAD FINANCIERA' AS classification,
       'requiere decision' AS repairability
FROM negative_amounts
UNION ALL
SELECT 'FIN-PAGO-INICIAL-VALOR-BASE', 'pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'importe_inicial debe preservar el original; valor_base contradictorio requiere explicacion',
       'El original pudo mutarse o incorporar ajustes sin trazabilidad', 'INTEGRIDAD FINANCIERA', 'requiere decision'
FROM payment_initial_mismatch
UNION ALL
SELECT 'FIN-PAGO-BALANCE', 'pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'monto_pagado + saldo_restante = importe_inicial',
       'La cabecera no reconcilia', 'INTEGRIDAD FINANCIERA', 'requiere decision'
FROM payment_balance_mismatch
UNION ALL
SELECT 'FIN-DETALLE-COBRO-MAYOR-PENDIENTE', 'detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'a_cobrar no supera importe_pendiente',
       'Aplicacion por encima de la deuda observable', 'SOBREPAGO', 'requiere decision'
FROM detail_over_pending
UNION ALL
SELECT 'FIN-PAGO-DETALLES-CABECERA', 'pagos/detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'La suma exacta de detalles activos coincide con monto de cabecera',
       'Dinero recibido y conceptos cobrados divergen', 'RECONCILIACION', 'requiere decision'
FROM payment_detail_mismatch
UNION ALL
SELECT 'FIN-MENSUALIDAD-BALANCE', 'mensualidades', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'monto_abonado + importe_pendiente = importe_inicial',
       'La deuda mensual no reconcilia', 'INTEGRIDAD FINANCIERA', 'requiere decision'
FROM monthly_balance_mismatch
UNION ALL
SELECT 'FIN-SALDO-CLAMPADO', 'pagos/mensualidades', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Un saldo cero no oculta un sobrepago',
       'Diferencia negativa perdida por clamp', 'SOBREPAGO', 'requiere decision'
FROM clamped_balances
UNION ALL
SELECT 'FIN-SOBREPAGO-SIN-REPRESENTAR', 'pagos/mensualidades', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Todo exceso se rechaza o se representa con credito trazable',
       'Dinero recibido sin aplicacion o credito correlacionable', 'SOBREPAGO', 'requiere decision'
FROM unrepresented_overpayments
UNION ALL
SELECT 'FIN-CREDITO-SIN-RESPALDO', 'alumnos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Credito acumulado reconcilia con movimientos de origen',
       'V060 solo conserva un saldo agregado y no una tabla de movimientos', 'TRAZABILIDAD AUSENTE', 'no reparable automaticamente'
FROM credits_without_ledger
UNION ALL
SELECT 'FIN-PAGO-POSIBLE-CLON', 'pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Pagos parciales no se representan mediante cabeceras indistinguibles',
       'Posible duplicacion o clon sin marcador en pagos', 'CLON POTENCIAL', 'requiere decision'
FROM possible_payment_clones
UNION ALL
SELECT 'FIN-DETALLE-CLON', 'detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Un detalle no se clona para representar parcialidad',
       'Origen y saldo pueden contarse mas de una vez', 'CLON CONFIRMADO', 'requiere decision'
FROM cloned_details
UNION ALL
SELECT 'FIN-PARCIAL-MEDIANTE-COPIA', 'mensualidades/detalle_pagos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'La parcialidad conserva un cargo y registra aplicaciones',
       'Las copias impiden determinar original, pagado y pendiente', 'MODELO HEREDADO', 'requiere decision'
FROM partial_payment_copies
UNION ALL
SELECT 'FIN-CAJA-PAGOS-EGRESOS', 'caja/pagos/egresos', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(business_date::text ORDER BY business_date))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(
           'fecha=' || business_date || ',caja=' || cash_total || ',pagos=' || payment_total ||
           ',egresos=' || expense_total || ',diferencia=' || (cash_total - payment_total + expense_total)
           ORDER BY business_date))[1:10], ', '), ''),
       'Caja, pagos validos y egresos se muestran por separado y su diferencia exacta debe explicarse',
       'Fuentes heredadas contradictorias', 'RECONCILIACION', 'requiere decision'
FROM cash_mismatch
UNION ALL
SELECT 'FIN-STOCK-COBRADO-SIN-MOVIMIENTO', 'detalle_pagos/stocks', COUNT(*)::bigint,
       COALESCE(array_to_string((array_agg(example_id ORDER BY example_id))[1:10], ', '), ''),
       COALESCE(array_to_string((array_agg(observed_value ORDER BY example_id))[1:10], ', '), ''),
       'Todo stock cobrado tiene un efecto de inventario trazable',
       'V060 no tiene tabla de movimientos para probar el efecto', 'TRAZABILIDAD AUSENTE', 'no reparable automaticamente'
FROM charged_stock_without_movement
ORDER BY rule_id;
