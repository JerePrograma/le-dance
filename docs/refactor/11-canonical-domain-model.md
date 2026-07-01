# Modelo de dominio canónico

## Obligación y pago

`Cargo` es la obligación original y apunta exactamente a mensualidad, matrícula,
concepto, venta de stock o cargo origen de recargo. `Pago` representa dinero
recibido. `AplicacionPago` une pago y cargo con importe exacto y estado
APLICADA/REVERTIDA. El crédito no aplicado se registra en `MovimientoCredito`.

No se clonan deudas, no se muta el importe original y ninguna descripción se
interpreta como relación.

## Ledgers

- `MovimientoCaja`: ingreso, egreso, ajuste o reverso enlazado al movimiento
  original.
- `MovimientoCredito`: generación, consumo, ajuste o reverso.
- `MovimientoStock`: ingreso, venta o reverso; `VentaStock` conserva precio
  unitario histórico.

Los reversos son nuevas filas compensatorias y cada original admite a lo sumo
un reverso por constraint.

## Recibos

`Recibo` registra pago, storage key/hash y timestamps documentales.
`ReciboPendiente` registra tipo, estado técnico, intentos, próxima ejecución,
error resumido, idempotency key, claim y lease. El pago crea ambos en su
transacción; el worker realiza PDF/filesystem/email después del commit.

## Procesos periódicos

Mensualidad es única por inscripción/año/mes; matrícula por alumno/año;
asistencia por disciplina/período y alumno/fecha. Los schedulers bloquean IDs
activos en orden, leen los detalles en un conjunto fijo de consultas y pueden
repetirse sin duplicar resultados.

## Contratos

- API: DTO records, importes string decimal, `PageResponse` estable.
- Tiempo: `Clock` y zona configurada.
- Seguridad: catch-all administrador, salvo login/refresh/perfil.
- Persistencia: una V1, PostgreSQL 15 y Hibernate validate.
