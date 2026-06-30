# Modelo de dominio canónico

## 1. Alcance y fuente histórica

Este documento fija el modelo objetivo previo a modificar Flyway o JPA. La
decisión del propietario confirma que Le Dance no fue desplegado en producción,
que no existe una base externa a actualizar y que toda base anterior debe
recrearse. El historial anterior permanece consultable en el commit
`2764083a47303880b2e90dc6bcef807406b3ee32`; no forma parte del baseline
operativo soportado.

La reconstrucción conserva el monolito Spring existente y las capacidades
observables válidas. No agrega microservicios, mensajería distribuida, CQRS,
event sourcing, un segundo modelo de dominio ni interfaces para servicios con
una sola implementación.

## 2. Inventario del modelo heredado

### 2.1 Entidades y responsabilidad observada

| Capacidad | Entidades heredadas | Decisión |
|---|---|---|
| Alumnos | `Alumno` | Conservar, quitar saldos financieros mutables y cascadas históricas. |
| Inscripciones | `Inscripcion` | Conservar como historia; baja por estado y fecha. |
| Disciplinas | `Disciplina`, `DisciplinaHorario`, `Profesor`, `Salon` | Conservar. Los horarios son composición editable; inscripciones no. |
| Asistencias | `AsistenciaMensual`, `AsistenciaAlumnoMensual`, `AsistenciaDiaria` | Conservar como historia; sin borrado en cascada desde inscripción o disciplina. |
| Facturación | `Mensualidad`, `Matricula`, `Concepto`, `SubConcepto`, `Bonificacion`, `Recargo` | Conservar obligaciones/orígenes y catálogos, pero reemplazar importes pagados/saldos duplicados por `Cargo`. |
| Pagos | `Pago`, `DetallePago` | Reemplazar `DetallePago` por `AplicacionPago`; simplificar `Pago` a dinero recibido. |
| Caja | Consultas derivadas de `Pago` y `Egreso` | Incorporar `MovimientoCaja` inmutable. |
| Crédito | `Alumno.creditoAcumulado` | Eliminar; incorporar `MovimientoCredito` inmutable y saldo derivado. |
| Inventario | `Stock` | Conservar producto y stock proyectado; incorporar `MovimientoStock` inmutable. |
| Egresos | `Egreso` | Conservar con importe `BigDecimal`, anulación y movimiento de caja asociado. |
| Seguridad | `Usuario`, `Rol` | Conservar; usuarios con actividad se desactivan. |
| Notificaciones | `Notificacion` | Conservar para notificaciones de aplicación. |
| Procesos | `ProcesoEjecutado` | Eliminar; la idempotencia se expresa con claves únicas en los resultados del proceso. |
| Observaciones | `ObservacionProfesor` | Conservar. |

No se encontraron `@Embeddable`. Los enums persistidos por JPA son estados de
inscripción, asistencia, mensualidad, pago y detalle; en el esquema canónico
cada enum persistido tiene un `CHECK` explícito.

### 2.2 Consumidores heredados que condicionan el rediseño

* Los flujos de cobro están repartidos entre `PagoServicio`,
  `PaymentProcessor`, `DetallePagoServicio`, `PaymentCalculationServicio`,
  `DetallePagoResolver`, `MensualidadServicio` y `MatriculaServicio`.
* `DetallePago` enlaza simultáneamente pago, alumno, mensualidad, matrícula,
  stock, concepto, sub-concepto, bonificación y recargo. También guarda importes
  que duplican el estado de la obligación y utiliza `esClon`.
* Caja reconstruye ingresos desde pagos y detalles; no existe un ledger de
  movimientos que permita reversión y reconciliación deterministas.
* El crédito se guarda como saldo mutable en `Alumno`, sin movimientos de
  respaldo.
* El stock actual se modifica en el producto y no conserva todos los movimientos
  que explican la existencia.
* Recibos, almacenamiento y email se disparan desde el flujo de pago con
  `@Async`, sin registro transaccional durable.
* Los schedulers generan mensualidades, matrículas, asistencias y recargos. La
  idempotencia heredada usa consultas o `ProcesoEjecutado`, no siempre una
  unicidad de base.
* El frontend envía importes como `number`, reconstruye saldos y mantiene campos
  heredados (`esClon`, importe inicial/pendiente y relaciones inferidas por
  descripción).

El inventario detallado de asociaciones y consumidores está en
`12-relationship-matrix.md`.

## 3. Agregados canónicos

### 3.1 Alumno

`Alumno` es la identidad del estudiante y mantiene datos de contacto, estado y
fechas de alta/baja. No contiene deuda, cuota total ni crédito monetario. Sus
inscripciones, cargos, pagos, asistencias y movimientos son historia separada y
no se eliminan en cascada.

### 3.2 Oferta académica e inscripción

`Disciplina` referencia a `Profesor` y opcionalmente a `Salon`. Sus horarios son
composición real porque no tienen significado sin la disciplina y pueden
reemplazarse de forma controlada. Una inscripción enlaza alumno y disciplina,
guarda tarifa particular opcional, bonificación opcional y estado. La
combinación alumno/disciplina sólo puede tener una inscripción activa.

### 3.3 Asistencia

`AsistenciaMensual` identifica disciplina y período. `AsistenciaAlumnoMensual`
enlaza ese registro con una inscripción. `AsistenciaDiaria` registra fecha y
estado. Son historia: se corrigen por actualización de estado, no por borrado
en cascada desde entidades académicas.

### 3.4 Orígenes cobrables

`Mensualidad` representa la cuota de una inscripción y período. `Matricula`
representa la matrícula anual del alumno. Una venta de inventario se representa
por `VentaStock`, con producto, cantidad y precio unitario snapshot. Un cargo
manual usa un `Concepto` explícito. Cada origen cobrable genera exactamente un
`Cargo`, protegido por una FK única.

`Bonificacion` y `Recargo` son catálogos configurables. Los importes efectivos
del cargo quedan materializados como snapshot al emitirlo; cambiar un catálogo
no reescribe historia.

### 3.5 Cargo

`Cargo` es la única autoridad de la obligación económica:

* alumno;
* tipo (`MENSUALIDAD`, `MATRICULA`, `CONCEPTO`, `VENTA_STOCK`);
* descripción snapshot;
* importe original inmutable `NUMERIC(19,2)`;
* emisión y vencimiento;
* estado (`PENDIENTE`, `PARCIAL`, `PAGADO`, `ANULADO`);
* versión optimista;
* exactamente uno de `mensualidad_id`, `matricula_id`, `concepto_id` o
  `venta_stock_id`.

Un `CHECK` exige que el tipo coincida con la única FK de origen no nula. Las
cuatro FKs son reales; no existe `source_type + source_id`.

El saldo no se persiste como segunda autoridad:

```text
saldo(cargo) = importe_original - SUM(aplicaciones con estado APLICADA)
```

El estado del cargo se mantiene en la misma transacción para consultas y se
reconcilia contra esa fórmula en tests y auditorías.

### 3.6 Pago y aplicación

`Pago` representa dinero recibido de un alumno. Guarda fecha de negocio, método,
monto recibido, estado (`REGISTRADO`, `ANULADO`), usuario, clave de idempotencia,
observación y versión. No guarda valor base, importe inicial ni saldo restante.

`AplicacionPago` aplica parte de un pago a un cargo del mismo alumno. Guarda
importe, estado (`APLICADA`, `REVERTIDA`), fecha, usuario, versión y un motivo de
reversión opcional. La dupla pago/cargo es única; nuevas aplicaciones al mismo
cargo desde otro pago son válidas.

El caso de uso bloquea los cargos y el pago afectados, recalcula sumas con
`BigDecimal`, y rechaza:

* importes no positivos;
* aplicación mayor al saldo del cargo;
* suma aplicada mayor al monto recibido;
* alumno distinto entre pago y cargo;
* cargo anulado;
* pago anulado;
* idempotency key reutilizada con otro contenido.

Los límites también quedan respaldados por checks, uniques y locking
transaccional. La regla de suma cruzada requiere el caso de uso y pruebas
concurrentes porque un `CHECK` de fila no puede expresarla.

### 3.7 Sobrepago y crédito

Un pago puede exceder la suma aplicada únicamente cuando el excedente se
registra explícitamente como crédito en la misma transacción. No se clamplea ni
se crea crédito implícito.

`MovimientoCredito` es un ledger inmutable con tipos `GENERACION`, `CONSUMO`,
`REVERSO` y `AJUSTE`, importe positivo, referencia explícita al pago que lo
genera o consume cuando corresponde e idempotency key única. El saldo es la suma
firmada de movimientos; un consumo que exceda el disponible se rechaza bajo
lock del alumno. El campo heredado `Alumno.creditoAcumulado` desaparece.

### 3.8 Caja y egresos

`MovimientoCaja` es un ledger inmutable con tipos `INGRESO_PAGO`, `EGRESO`,
`REVERSO` y `AJUSTE`. Cada movimiento tiene importe positivo, fecha de negocio,
método, usuario, timestamps y una referencia explícita a pago o egreso. Un
constraint de origen e idempotency key impiden duplicar el efecto.

Registrar un pago crea su ingreso de caja en la misma transacción. Anularlo
crea un movimiento compensatorio, sin editar ni borrar el original. Registrar o
anular un egreso sigue la misma regla.

### 3.9 Inventario

`Stock` conserva el producto y la proyección `cantidad_actual`; el precio es
`BigDecimal`. `MovimientoStock` registra `INGRESO`, `VENTA`, `REVERSO` o
`AJUSTE`, cantidad positiva, origen explícito e idempotency key. Una venta crea
`VentaStock`, su movimiento y su cargo en una transacción. No se permite una
proyección negativa. La reconciliación compara `cantidad_actual` con la suma
firmada del ledger.

### 3.10 Recibo y efectos externos

`Recibo` identifica de forma única un pago, estado de generación/envío, ruta o
clave de almacenamiento, intentos, último error seguro y timestamps. La
transacción del pago inserta un trabajo `ReciboPendiente`; un worker local lo
reclama después del commit, genera/almacena el PDF y envía el email mediante los
puertos externos ya existentes.

La outbox es específica a recibos; no se crea un bus genérico. La combinación
`pago_id + tipo_efecto` es única. Reintentos son idempotentes y el pago válido no
se revierte por fallos externos.

## 4. Dinero, porcentaje y redondeo

* Todo importe usa `BigDecimal` en Java y `NUMERIC(19,2)` en PostgreSQL.
* Todo porcentaje usa `BigDecimal` y `NUMERIC(7,4)`, entre `0` y `100`.
* La escala monetaria canónica es 2 y el redondeo es `HALF_UP` en el punto donde
  una regla produce moneda.
* Los DTOs JSON transportan dinero como strings decimales con dos posiciones.
* No se convierte a `double`, no se usa igualdad de objetos para comparar y no
  hay tolerancias flotantes.

## 5. Fechas y auditoría

Las fechas de negocio son `LocalDate/DATE`, obtenidas desde un único bean
`Clock` configurado con `app.time-zone`. Los eventos técnicos usan
`Instant/TIMESTAMPTZ`. No se crea un wrapper de `Clock`.

Las entidades transaccionales usan `created_at`, `updated_at`, `created_by` sólo
cuando hay una operación humana que auditar y `@Version` cuando existe riesgo de
concurrencia. Los catálogos simples no reciben auditoría ceremonial.

## 6. Ciclo de vida y borrado

Las asociaciones se clasifican en la matriz. La regla por defecto es `RESTRICT`.

* Historia financiera, inscripciones y asistencias: nunca se borra físicamente.
* Alumnos, usuarios, disciplinas, profesores, stocks y catálogos referenciados:
  baja lógica.
* Horarios de una disciplina: composición A; se permite borrado controlado al
  sustituirlos porque no son historia financiera ni asistencia registrada.
* `SET NULL` sólo se usa cuando la referencia es opcional y el snapshot conserva
  significado, por ejemplo usuario desactivado nunca se elimina y por tanto no
  necesita `SET NULL`.
* No se utiliza `ON DELETE CASCADE` en historia.

## 7. Transacciones e idempotencia

Los límites transaccionales coinciden con casos de uso:

1. emitir cargo;
2. registrar pago y aplicaciones;
3. anular pago y compensar aplicaciones/caja/crédito/stock;
4. registrar o anular egreso;
5. vender o revertir stock;
6. generar mensualidades/matrículas del período.

Las claves únicas son la autoridad de idempotencia:

* pago: `idempotency_key`;
* reversión: una reversión por pago;
* mensualidad: inscripción + período;
* matrícula: alumno + año;
* asistencia mensual: disciplina + período;
* movimiento: tipo + origen o `idempotency_key`;
* recibo/outbox: pago + tipo de efecto.

No se conserva `ProcesoEjecutado` como sustituto de esas restricciones.

## 8. Contrato API y frontend

Los controladores devuelven records de respuesta, nunca entidades. El contrato
financiero canónico expone:

* cargos del alumno con importe original, aplicado, saldo y estado calculados
  por backend;
* registro de pago con `idempotencyKey`, `metodoPagoId`, `montoRecibido` y lista
  de `{cargoId, importe}`;
* anulación con motivo e idempotency key;
* recibo por `pagoId`;
* caja, crédito y stock como consultas de movimientos/proyecciones.

El frontend sólo valida formato y presenta totales devueltos por backend. No
decide saldos, estados ni sobrepagos. Descripciones son snapshots visuales, no
identificadores.

## 9. Estructuras que no forman parte del modelo canónico

Se eliminan del esquema y del código activo:

* `detalle_pagos` y `DetallePago`;
* `es_clon` y toda clonación de mensualidad/detalle;
* saldo, importe inicial y valor base duplicados en `Pago`;
* saldo mutable de crédito en `Alumno`;
* `ProcesoEjecutado`;
* relaciones financieras reconstruidas desde descripciones;
* cascadas desde alumno/disciplina/inscripción hacia historia;
* cualquier tabla recreada o abandonada sin consumidor vigente.

La lista definitiva de tablas se fija en el plan de V1 y se valida mediante el
test de catálogo PostgreSQL.
