# Plantilla de ejecución de auditoría canónica

Esta plantilla sólo se ejecuta contra PostgreSQL 15 desechable. No usar
`localhost:5432` ni una base externa.

## Identificación

- fecha/hora:
- commit:
- imagen PostgreSQL:
- puerto aleatorio:
- versión Flyway esperada: `1`:
- cantidad de migraciones esperada: `1`:

## Gates

1. Flyway sobre esquema vacío: resultado y duración.
2. `ddl-auto=validate`: resultado.
3. Catálogo: columnas, NUMERIC, FKs RESTRICT, uniques y checks.
4. Reconciliación sintética: cargos sin origen, aplicaciones sobreaplicadas,
   reversos duplicados, movimientos sin origen, recibos/outbox duplicados.
5. Plan de cargos: dataset, query, nodo, índice, sort, buffers, filas y tiempo.
6. Concurrencia: pagos, schedulers y claims outbox.

## Consultas mínimas

```sql
SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT count(*) FROM flyway_schema_history WHERE success;
SELECT cargo_id, sum(importe_aplicado) FROM aplicaciones_pago
WHERE estado = 'APLICADA' GROUP BY cargo_id;
SELECT pago_id, count(*) FROM recibos GROUP BY pago_id HAVING count(*) > 1;
SELECT pago_id, tipo, count(*) FROM recibos_pendientes
GROUP BY pago_id, tipo HAVING count(*) > 1;
```

Registrar filas devueltas; no reemplazar cero resultados por una afirmación sin
salida. Adjuntar el reporte Surefire del gate que materializa estas consultas.
