# Invariantes de negocio

Estado: contrato de estabilización previo a modificar cálculos o datos. Donde el sistema actual no implementa la regla, se marca como riesgo y no como comportamiento certificado.

## Seguridad

- Sólo un administrador autenticado puede crear usuarios o asignar/modificar roles.
- El primer administrador se crea por un bootstrap explícito, de un solo uso y sin contraseña fija versionada.
- Access y refresh tokens no son intercambiables.
- Token inválido, vencido, con issuer/firma incorrectos o usuario inactivo produce 401.
- Usuario autenticado sin permiso produce 403 y conserva su sesión frontend.
- Un recibo requiere autorización sobre el alumno/pago o un token temporal no enumerable.

## Historial

- Dar de baja alumno, inscripción, disciplina, usuario, pago, mensualidad o detalle no elimina registros financieros, académicos ni de auditoría.
- Una anulación es una transición explícita y auditable; no una eliminación física.
- Descripciones son snapshots de visualización y nunca claves de relación.
- Toda relación de origen financiero usa ID/FK explícita.

## Mensualidad/cargo

- `importeInicial` es inmutable una vez emitido.
- `montoAbonado` es la suma autoritativa de aplicaciones válidas no revertidas.
- `importePendiente = importeInicial - montoAbonado` con escala monetaria definida.
- Un pendiente positivo implica estado pendiente/activo; cero implica pagado; negativo no se silencia.
- Sobrepago se rechaza o genera crédito explícito; nunca se clampa a cero sin registrar diferencia.
- Bonificación y recargo se registran con regla, importe aplicado y snapshot.
- Fecha de pago deriva del momento de liquidación total o de una regla explícita para pagos parciales.
- Reversión resta aplicaciones y efectos de caja/crédito sin mutar el importe original.

## Pago y aplicaciones

- `importeRecibido` es el dinero recibido; no cambia para representar saldo.
- La suma de aplicaciones más crédito generado debe reconciliar con el importe recibido.
- Cada aplicación relaciona pago y cargo por ID y tiene importe positivo.
- No se clonan pagos, detalles o mensualidades para representar pagos parciales.
- No se permite aplicar más que el pendiente salvo una política explícita de crédito.
- Un pago válido, sus aplicaciones y su movimiento de caja se persisten en una transacción.
- PDF/correo ocurren después del commit; un fallo externo no revierte un pago válido ni duplica recibos.

## Caja

- Todo ingreso/egreso tiene importe `BigDecimal`, método, usuario, fecha/hora, estado y referencia de origen.
- Los totales son una proyección de movimientos, no una segunda fuente editable.
- Una anulación crea/revierte el efecto de manera auditable.
- No hay diferencias inexplicadas entre pagos válidos, egresos y movimientos.

## Stock

- El stock actual deriva de movimientos o se reconcilia con ellos.
- Un cobro/venta y su movimiento de stock son transaccionalmente consistentes.
- Stock negativo se rechaza salvo ajuste explícito autorizado y auditado.
- Anulación revierte exactamente el movimiento original; no infiere producto por descripción.

## Crédito

- El crédito pertenece a un alumno y se registra mediante movimientos con referencia.
- Crédito no se acumula por mutaciones opacas de un saldo `Double`.
- Generación, consumo y reversión reconcilian a cero con el saldo proyectado.

## Bajas académicas

- La baja de alumno conserva inscripciones, asistencias, mensualidades, matrículas, pagos, recibos y caja.
- La baja de inscripción impide nuevas obligaciones/asistencias pero conserva las existentes.
- La baja de disciplina impide nuevas inscripciones y conserva su historial.

## Schedulers y tiempo

- La fecha de negocio proviene de un `Clock` y zona IANA configurables.
- Generar mensualidad, matrícula, asistencia o recargo es idempotente por constraint de base de datos.
- Reintentos o ejecuciones concurrentes no crean duplicados.
- `ProcesoEjecutado` puede observar resultados, pero no reemplaza constraints.

## Invariantes técnicas

- Flyway es la fuente de esquema; `ddl-auto=validate` en todos los entornos representativos.
- V1..V060 son inmutables y toda evolución empieza en V061.
- Dinero usa `BigDecimal`, escala explícita, `HALF_UP` salvo regla documentada y `compareTo`.
- DTOs públicos no exponen entidades JPA ni mutan colecciones para serializar.
- El backend recalcula y valida todo importe enviado por el frontend.
- Cada operación crítica tiene prueba de regresión y reconciliación reproducible.
