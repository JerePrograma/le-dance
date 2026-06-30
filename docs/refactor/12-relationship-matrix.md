# Matriz de relaciones

## 1. Convenciones

Esta matriz fue levantada antes de modificar Flyway o JPA. `Actual` describe el
commit `2764083a47303880b2e90dc6bcef807406b3ee32`; `Canónica` es la decisión que
debe implementar la nueva V1.

Clasificación:

* **A**: composición real eliminable con el padre.
* **B**: relación histórica que nunca se elimina en cascada.
* **C**: relación opcional preservable con `SET NULL`.
* **D**: catálogo o agregado independiente con `RESTRICT`.
* **E**: relación derivada que desaparece.
* **F**: relación textual ilegítima convertida a FK.
* **G**: estructura heredada sin consumidor que desaparece.

Abreviaturas de consumidores: `FE alumno`, `FE disciplina`, `FE asistencia`,
`FE pagos`, `FE caja`, `FE stock`, `FE seguridad` son las pantallas y módulos API
homónimos. `Sin prueba` significa que no había una regresión de relación
específica en el baseline, no que no existieran tests de la clase.

## 2. Relaciones académicas y de seguridad

| Clase | Agregado origen | Entidad/tabla origen | Propiedad; cardinalidad; propietario/mappedBy | FK; null; índice/unique actual | Cascade; orphan; ON DELETE actual | Ciclo, baja y borrado | Modificación (endpoint; servicio) | Consulta; consumidor; pruebas | Decisión canónica |
|---|---|---|---|---|---|---|---|---|---|
| B | Alumno | `Alumno/alumnos` | `inscripciones`; 1:N; inverso/`alumno` | `inscripciones.alumno_id`; NN; índice no garantizado; unique no | ALL; sí; FK heredada CASCADE | Historia; baja alumno; no borrar | `DELETE /api/alumnos/{id}`; `AlumnoServicio` | `InscripcionRepositorio`; FE alumno/inscripción; caracterización parcial | Quitar cascade/orphan. FK indexada `RESTRICT`; baja preserva inscripciones. |
| B | Alumno | `Alumno/alumnos` | `matriculas`; 1:N; inverso/`alumno` | `matriculas.alumno_id`; NN; índice no garantizado; `(alumno,anio)` ausente | ALL; sí; FK heredada CASCADE | Historia anual; no borrar | baja alumno y scheduler; `AlumnoServicio/MatriculaServicio` | `MatriculaRepositorio`; FE alumno/pagos; sin prueba relacional | Quitar cascade/orphan. `RESTRICT`, índice y unique alumno+año. |
| D | Disciplina | `Disciplina/disciplinas` | `salon`; N:1; propietario | `salon_id`; nullable; índice no garantizado | default; no; heredado RESTRICT | Catálogo opcional; desactivar; no borrar si usado | POST/PUT disciplina; `DisciplinaServicio` | `DisciplinaRepositorio`; FE disciplina; tests de servicio | Mantener opcional, FK indexada `RESTRICT`; salón se desactiva. |
| D | Disciplina | `Disciplina/disciplinas` | `profesor`; N:1; propietario | `profesor_id`; NN; índice no garantizado | default; no; heredado RESTRICT | Agregado independiente; desactivar | POST/PUT disciplina; `DisciplinaServicio` | repos disciplina/horario; FE disciplina/profesor; tests parciales | Mantener NN, FK indexada `RESTRICT`. |
| B | Disciplina | `Disciplina/disciplinas` | `inscripciones`; 1:N; inverso/`disciplina` | `inscripciones.disciplina_id`; NN; índice no garantizado | ALL; sí; heredado CASCADE | Historia; baja disciplina; nunca borrar | DELETE disciplina; `DisciplinaServicio` | `InscripcionRepositorio`; FE disciplina; sin prueba de preservación | Quitar cascade/orphan. `RESTRICT`; baja lógica. |
| A | Disciplina | `Disciplina/disciplinas` | `horarios`; 1:N; inverso/`disciplina` | `disciplina_horarios.disciplina_id`; NN; índice no garantizado | ALL; sí; heredado CASCADE | Composición de configuración; reemplazo explícito | PUT disciplina; `DisciplinaHorarioServicio` | `DisciplinaHorarioRepositorio`; FE disciplina; tests de horario | Única composición eliminable. Mantener orphan controlado y CASCADE sólo aquí; índice FK y unique disciplina+día+hora. |
| D | DisciplinaHorario | `DisciplinaHorario/disciplina_horarios` | `disciplina`; N:1; propietario | `disciplina_id`; NN; índice/unique como arriba | default; no; heredado CASCADE | Pertenece a disciplina; se elimina sólo por edición de horarios | PUT disciplina; `DisciplinaHorarioServicio` | repo horario; FE disciplina; tests parciales | FK `ON DELETE CASCADE` autorizada sólo para composición A. |
| B | Inscripción | `Inscripcion/inscripciones` | `alumno`; N:1; propietario | `alumno_id`; NN; índice; active unique ausente | default; no; heredado CASCADE | Historia; estado/fecha baja; no delete | POST/PUT/DELETE inscripción; `InscripcionServicio` | `InscripcionRepositorio`; FE alumno/inscripción; tests de servicio | FK `RESTRICT`, índice; unique parcial alumno+disciplina para activa. |
| B | Inscripción | `Inscripcion/inscripciones` | `disciplina`; N:1; propietario | `disciplina_id`; NN; índice | default; no; heredado CASCADE | Historia; estado/fecha baja | endpoints inscripción; `InscripcionServicio` | repo inscripción; FE disciplina; tests parciales | FK `RESTRICT`, índice compuesto para activas. |
| D | Inscripción | `Inscripcion/inscripciones` | `bonificacion`; N:1 opcional; propietario | `bonificacion_id`; null; índice no garantizado | default; no; heredado RESTRICT | Catálogo snapshot al emitir cargo; desactivar | POST/PUT inscripción; `InscripcionServicio` | repo inscripción; FE inscripción; sin prueba FK | Mantener opcional `RESTRICT`, indexar; catálogo no se borra si usado. |
| B | Inscripción | `Inscripcion/inscripciones` | `mensualidades`; 1:N; inverso/`inscripcion` | `mensualidades.inscripcion_id`; NN; unique período ausente | ALL; sí; heredado CASCADE | Historia financiera; no borrar | scheduler/POST mensualidades; `MensualidadServicio` | repo mensualidad; FE mensualidades; tests heredados | Quitar cascade/orphan. `RESTRICT`; unique inscripción+año+mes. |
| B | Inscripción | `Inscripcion/inscripciones` | `asistenciasAlumnoMensual`; 1:N; inverso/`inscripcion` | `asistencias_alumno_mensual.inscripcion_id`; NN; índice | ALL; sí; CASCADE | Historia de asistencia; no borrar | scheduler/asistencia; `AsistenciaMensualServicio` | repos asistencia; FE asistencia; tests parciales | Quitar cascade/orphan y `ON DELETE CASCADE`; `RESTRICT`. |
| D | Profesor | `Profesor/profesores` | `usuario`; 1:1 opcional; propietario | `usuario_id`; null; unique no explícito | default; no; heredado RESTRICT | Usuario independiente; ambos se desactivan | PUT profesor/usuario; servicios respectivos | repos profesor/usuario; FE profesor/seguridad; tests parciales | Mantener opcional, unique y FK indexada `RESTRICT`. |
| B | Profesor | `Profesor/profesores` | `disciplinas`; 1:N; inverso/`profesor` | `disciplinas.profesor_id`; NN; índice | ninguno; no; RESTRICT | Profesor desactivable; no borrar historia | DELETE profesor; `ProfesorServicio` | repo disciplina; FE profesor; tests parciales | Mantener sin cascade; `RESTRICT`. |
| B | Profesor | `ObservacionProfesor/observaciones_profesores` | `profesor`; N:1; propietario | `profesor_id`; NN; índice no garantizado | default; no; heredado RESTRICT | Historia; no borrar | endpoints observaciones; `ObservacionProfesorServicio` | repo observación; FE profesor; tests controller | FK indexada `RESTRICT`; observación no se elimina físicamente. |
| D | Usuario | `Usuario/usuarios` | `rol`; N:1; propietario | `rol_id`; NN; índice no garantizado | default; no; heredado RESTRICT | Rol catálogo estructural; usuario desactivable | endpoints usuarios/roles; `UsuarioServicio` | `UsuarioRepositorio`; FE seguridad; MockMvc seguridad | FK indexada `RESTRICT`; username normalizado unique. |

## 3. Relaciones de asistencia

| Clase | Agregado origen | Entidad/tabla origen | Propiedad; cardinalidad; propietario/mappedBy | FK; null; índice/unique actual | Cascade; orphan; ON DELETE actual | Ciclo, baja y borrado | Modificación (endpoint; servicio) | Consulta; consumidor; pruebas | Decisión canónica |
|---|---|---|---|---|---|---|---|---|---|
| B | Asistencia | `AsistenciaMensual/asistencias_mensuales` | `disciplina`; N:1; propietario | `disciplina_id`; NN; índice; unique período incompleto | default; no; RESTRICT | Historia mensual; corrección por estado | endpoints/scheduler asistencia; `AsistenciaMensualServicio` | repos asistencia; FE asistencia; integración parcial | `RESTRICT`; unique disciplina+año+mes e índice. |
| B | Asistencia | `AsistenciaMensual/asistencias_mensuales` | `asistenciasAlumnoMensual`; 1:N; inverso/`asistenciaMensual` | `asistencia_mensual_id`; NN; índice | ALL; sí; CASCADE | Historia; no borrar en cascada | creación mensual; `AsistenciaMensualServicio` | repo asistencia alumno; FE asistencia; tests parciales | Quitar orphan y cascade destructivo; `RESTRICT`. |
| B | Asistencia | `AsistenciaAlumnoMensual/asistencias_alumno_mensual` | `inscripcion`; N:1; propietario | `inscripcion_id`; NN; índice | default + Hibernate CASCADE DB; no | Historia ligada a inscripción | generación asistencia; `AsistenciaMensualServicio` | repo asistencia; FE asistencia; tests parciales | Quitar `@OnDelete`; `RESTRICT`; unique asistencia mensual+inscripción. |
| B | Asistencia | `AsistenciaAlumnoMensual/asistencias_alumno_mensual` | `asistenciaMensual`; N:1; propietario | `asistencia_mensual_id`; NN; índice | default + Hibernate CASCADE DB; no | Historia | generación asistencia; servicio asistencia | repo asistencia; FE asistencia | Quitar `@OnDelete`; `RESTRICT`. |
| B | Asistencia | `AsistenciaAlumnoMensual/asistencias_alumno_mensual` | `asistenciasDiarias`; 1:N; inverso/`asistenciaAlumnoMensual` | `asistencias_diarias.asistencia_alumno_mensual_id`; NN; índice | ALL; sí; CASCADE | Historia diaria; corregir, no borrar | PUT/DELETE asistencia diaria; `AsistenciaDiariaServicio` | repo diaria; FE asistencia; controller tests | Quitar delete endpoint/cascade/orphan; `RESTRICT`; unique padre+fecha. |
| B | Asistencia | `AsistenciaDiaria/asistencias_diarias` | `asistenciaAlumnoMensual`; N:1; propietario | FK anterior; NN; índice | default; no; CASCADE | Historia | PUT asistencia; servicio diario | repo diaria; FE asistencia | `RESTRICT`, estado con CHECK. |

## 4. Relaciones financieras heredadas y sustitución

| Clase | Agregado origen | Entidad/tabla origen | Propiedad; cardinalidad; propietario/mappedBy | FK; null; índice/unique actual | Cascade; orphan; ON DELETE actual | Ciclo, baja y borrado | Modificación (endpoint; servicio) | Consulta; consumidor; pruebas | Decisión canónica |
|---|---|---|---|---|---|---|---|---|---|
| D | Mensualidad | `Mensualidad/mensualidades` | `inscripcion`; N:1; propietario | `inscripcion_id`; NN; índice/unique período ausente | default; no; heredado CASCADE | Obligación origen; no borrar | generar/DELETE mensualidad; `MensualidadServicio` | repo mensualidad; FE mensualidades/pagos; caracterización | Conservar origen, eliminar DELETE, `RESTRICT`, unique período. Cargo 1:1 explícito. |
| D | Mensualidad | `Mensualidad/mensualidades` | `bonificacion`; N:1 opcional | `bonificacion_id`; null; índice no garantizado | default; no; RESTRICT | Catálogo; snapshot en cargo | generar mensualidad; servicio mensualidad | repo mensualidad; FE | `RESTRICT`; mantener referencia informativa e importe final en Cargo. |
| D | Mensualidad | `Mensualidad/mensualidades` | `recargo`; N:1 opcional | `recargo_id`; null; índice no garantizado | default; no; RESTRICT | Catálogo; snapshot | scheduler recargo; `RecargoServicio` | repos mensualidad; FE | `RESTRICT`; cargo se ajusta mediante ajuste explícito antes de cobro, no mutación ambigua. |
| E | Mensualidad | `Mensualidad/mensualidades` | `detallePagos`; 1:N inverso | `detalle_pagos.mensualidad_id`; null | ALL; sí; CASCADE heredado | Mezcla deuda/pago; clonable | pagos/mensualidad; múltiples servicios | repo detalle; FE pagos; tests heredados | Eliminar. Sustituir por `Cargo` 1:1 y aplicaciones Pago-Cargo. |
| B | Matrícula | `Matricula/matriculas` | `alumno`; N:1 | `alumno_id`; NN; índice/unique año ausente | default; no; CASCADE desde alumno | Historia anual | scheduler/PUT matrícula; `MatriculaServicio` | repo matrícula; FE pagos; tests heredados | `RESTRICT`; unique alumno+año; Cargo 1:1. |
| B | Pago | `Pago/pagos` | `alumno`; N:1 | `alumno_id`; NN; índice | default; no; heredado RESTRICT | Dinero recibido; anular, no borrar | POST/PUT/DELETE pago; `PagoServicio/PaymentProcessor` | `PagoRepositorio`; FE pagos/caja; caracterización | Conservar NN `RESTRICT`; POST idempotente; reemplazar DELETE/PUT financiero por anulación explícita. |
| D | Pago | `Pago/pagos` | `metodoPago`; N:1 opcional | `metodo_pago_id`; null; índice | default; no; RESTRICT | Catálogo snapshot necesario; método requerido para registro | POST/PUT pago; pago services | repo pago/caja; FE pagos; tests parciales | Hacer NN para pagos registrados; FK indexada `RESTRICT`. |
| B | Pago | `Pago/pagos` | `usuario`; N:1 opcional | `usuario_id`; null; índice | default; no; RESTRICT | Actor histórico; usuario nunca se borra | POST pago; pago services | repo pago; FE recibo; seguridad parcial | Hacer NN en operaciones autenticadas/bootstrap explícito; `RESTRICT`. |
| E | Pago | `Pago/pagos` | `detallePagos`; 1:N inverso | `detalle_pagos.pago_id`; NN; índice | ALL; sí; CASCADE | Mezcla líneas, aplicaciones y deuda; se borra con pago | POST/PUT/DELETE pago; processor | repo detalle; FE pagos/recibo/caja; tests heredados | Eliminar `DetallePago`; sustituir por `AplicacionPago` histórica sin cascade. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `pago`; N:1 | `pago_id`; NN; índice | default; no; CASCADE | Línea mutable/clonada | pago endpoints; processor | repo detalle; FE pagos | Tabla y entidad eliminadas. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `mensualidad`; N:1 opcional | `mensualidad_id`; null; índices inconsistentes | default; no; CASCADE | Relación parcial junto con descripción | pago/mensualidad services | queries detalle; FE | Reemplazar por Cargo con `mensualidad_id` unique real. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `matricula`; N:1 opcional | `matricula_id`; null | default; no; CASCADE | Ídem | servicios pago/matrícula | repo detalle | Reemplazar por Cargo con `matricula_id` unique. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `stock`; N:1 opcional | `stock_id`; null | default; no; CASCADE/RESTRICT variable | Ídem; cantidad en texto | servicios pago/stock | repo detalle; FE stock/pago | Reemplazar por `VentaStock` + Cargo + MovimientoStock. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `concepto`; N:1 opcional | `concepto_id`; null | default; no; RESTRICT | Catálogo y obligación mezclados | pago/detalle services | repos concepto/detalle; FE pago | Reemplazar por Cargo `concepto_id`; descripción sólo snapshot. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `subConcepto`; N:1 opcional | `subconcepto_id`; null | default; no; RESTRICT | Redundante: concepto ya posee sub-concepto | pago/detalle services | repo detalle | Eliminar relación redundante. Se obtiene por Concepto para catálogo. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `bonificacion`; N:1 opcional | `bonificacion_id`; null | default; no; RESTRICT | Duplicada con origen; mutable | pago/detalle services | repo detalle | Eliminar de aplicación; guardar snapshot de cálculo en Cargo. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `recargo`; N:1 opcional | `recargo_id`; null | default; no; RESTRICT | Duplicada con origen | pago/detalle services | repo detalle | Eliminar de aplicación; snapshot en Cargo. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `alumno`; N:1 | `alumno_id`; NN | default; no; RESTRICT | Redundante con Pago/origen | processor | repo detalle por alumno | Eliminar; alumno se valida por Pago y Cargo. |
| E | Pago heredado | `DetallePago/detalle_pagos` | `usuario`; N:1 opcional | `usuario_id`; null | default; no; RESTRICT | Actor ambiguo | processor | repo detalle | Actor explícito en AplicacionPago. |
| D | Concepto | `Concepto/conceptos` | `subConcepto`; N:1 | `sub_concepto_id`; NN; índice | default; no; RESTRICT | Catálogo jerárquico; desactivar | endpoints concepto/subconcepto; services | repos catálogo; FE conceptos; tests CRUD | Mantener `RESTRICT`; ambos con `activo`; no borrado si referidos. |
| D | Egreso | `Egreso/egresos` | `metodoPago`; N:1 opcional | `metodo_pago_id`; null; índice | default; no; RESTRICT | Historia de caja | POST/PUT/DELETE egreso; `EgresoServicio` | repo egreso/caja; FE caja; tests parciales | Hacer NN; anulación, no delete; MovimientoCaja 1:1 por origen. |

## 5. Nuevas relaciones canónicas

| Clase | Agregado origen | Entidad/tabla origen | Propiedad; cardinalidad; propietario/mappedBy | FK; null; índice/unique | Cascade; orphan; ON DELETE | Ciclo, baja y borrado | Modificación (endpoint; servicio) | Consulta; consumidor; pruebas requeridas | Decisión canónica |
|---|---|---|---|---|---|---|---|---|---|
| B | Cargo | `Cargo/cargos` | `alumno`; N:1 propietario | `alumno_id`; NN; índice `(alumno,estado,vencimiento)` | none; no; RESTRICT | Historia financiera inmutable/anulable | emisión y pagos; `CargoServicio` | `CargoRepositorio`; FE pagos; PostgreSQL + API | Autoridad de obligación. |
| D | Cargo | `Cargo/cargos` | `mensualidad`; 0..1:1 propietario | `mensualidad_id`; null; unique parcial | none; no; RESTRICT | Origen histórico | scheduler mensual; facturación service | cargo repo; FE mensualidad; unique/FK | Exactamente un origen según tipo. |
| D | Cargo | `Cargo/cargos` | `matricula`; 0..1:1 propietario | `matricula_id`; null; unique parcial | none; no; RESTRICT | Origen histórico | scheduler anual | cargo repo; FE pagos | Exactamente un origen según tipo. |
| D | Cargo | `Cargo/cargos` | `concepto`; 0..1:N propietario | `concepto_id`; null; índice | none; no; RESTRICT | Catálogo desactivable | POST cargo manual | cargo/concepto repos; FE pagos | Tipo CONCEPTO exige FK; descripción snapshot no resuelve ID. |
| D | Cargo | `Cargo/cargos` | `ventaStock`; 0..1:1 propietario | `venta_stock_id`; null; unique | none; no; RESTRICT | Venta y obligación históricas | venta/cobro; `StockServicio/CargoServicio` | repos stock/cargo; FE stock | Tipo VENTA_STOCK exige FK. |
| B | Aplicación | `AplicacionPago/aplicaciones_pago` | `pago`; N:1 propietario | `pago_id`; NN; índice; unique pago+cargo | none; no; RESTRICT | Historia; revertir, no borrar | POST pago/anulación; `PagoServicio` | aplicación repo; FE pagos/recibo; concurrencia | Suma aplicada no excede pago. |
| B | Aplicación | `AplicacionPago/aplicaciones_pago` | `cargo`; N:1 propietario | `cargo_id`; NN; índice `(cargo,estado)` | none; no; RESTRICT | Historia; revertir | pago/anulación | aplicación/cargo repos; FE pagos; concurrencia | Suma activa no excede cargo. |
| B | Aplicación | `AplicacionPago/aplicaciones_pago` | `usuario`; N:1 propietario | `usuario_id`; NN; índice | none; no; RESTRICT | Actor auditable | pago/anulación | repo usuario/aplicación; FE recibo; seguridad | Usuario se desactiva, no borra. |
| B | Caja | `MovimientoCaja/movimientos_caja` | `pago`; 0..1:N propietario | `pago_id`; null; índice/unique por tipo+origen | none; no; RESTRICT | Ledger inmutable/compensatorio | pago/anulación; `PagoServicio` | caja repo; FE caja; integración | Origen y tipo coherentes por CHECK. |
| B | Caja | `MovimientoCaja/movimientos_caja` | `egreso`; 0..1:N propietario | `egreso_id`; null; índice/unique por tipo+origen | none; no; RESTRICT | Ledger inmutable | egreso/anulación; `EgresoServicio` | caja repo; FE caja | Origen explícito. |
| D | Caja | `MovimientoCaja/movimientos_caja` | `metodoPago`; N:1 | `metodo_pago_id`; NN; índice | none; no; RESTRICT | Catálogo histórico | casos anteriores | caja repo; FE caja | Sin SET NULL: catálogo se desactiva. |
| B | Crédito | `MovimientoCredito/movimientos_credito` | `alumno`; N:1 | `alumno_id`; NN; índice `(alumno,fecha)` | none; no; RESTRICT | Ledger inmutable | pago/crédito; `PagoServicio` | crédito repo; FE pagos; concurrencia | Saldo derivado; consumo bajo lock. |
| B | Crédito | `MovimientoCredito/movimientos_credito` | `pago`; N:1 opcional | `pago_id`; null; índice/unique por tipo+origen | none; no; RESTRICT | Generación/consumo/reverso | pago/anulación | crédito repo; FE | FK explícita; ajustes sin pago exigen motivo. |
| B | Stock | `VentaStock/ventas_stock` | `stock`; N:1 | `stock_id`; NN; índice | none; no; RESTRICT | Venta histórica | POST venta/pago | stock/venta repos; FE stock; integración | Precio unitario snapshot BigDecimal. |
| B | Stock | `MovimientoStock/movimientos_stock` | `stock`; N:1 | `stock_id`; NN; índice `(stock,fecha)` | none; no; RESTRICT | Ledger inmutable | ingreso/venta/reverso; `StockServicio` | stock movement repo; FE stock; concurrencia | Proyección se reconcilia. |
| B | Stock | `MovimientoStock/movimientos_stock` | `ventaStock`; N:1 opcional | `venta_stock_id`; null; índice/unique tipo+origen | none; no; RESTRICT | Origen explícito | venta/reverso | movement repo | Venta y reverso no duplicables. |
| B | Recibo | `Recibo/recibos` | `pago`; 1:1 | `pago_id`; NN; unique | none; no; RESTRICT | Documento histórico; estados/reintento | GET recibo/worker; `ReciboServicio` | recibo repo; FE recibo; side-effect tests | Un recibo por pago. |
| B | Recibo | `ReciboPendiente/recibos_pendientes` | `pago`; N:1 | `pago_id`; NN; unique pago+tipo | none; no; RESTRICT | Outbox durable específica | commit pago/worker | outbox repo; sin consumidor directo; after-commit tests | No bus genérico; retry con attempts/next_attempt_at. |

## 6. Relaciones textuales e inferidas

| Clase | Origen actual | Consumidores encontrados | Riesgo | Decisión |
|---|---|---|---|---|
| F | `DetallePago.descripcionConcepto` y `cuotaOCantidad` se usan junto a búsquedas/parsing para decidir mensualidad, matrícula, stock o concepto. | `DetallePagoResolver`, `PaymentProcessor`, `PagoServicio`, `MensualidadServicio`, reportes y frontend de pagos. | Texto editable funciona como FK, permite origen equivocado y saldo no determinista. | Eliminar el resolver textual. El request exige `cargoId`; Cargo posee FK de origen real y descripción snapshot sólo visual. |
| E | `DetallePago.alumno` duplica `Pago.alumno` y el alumno del origen. | Queries de cobranza y reportes. | Asociaciones contradictorias representables. | Eliminar la relación; consulta por `Pago.alumno`/`Cargo.alumno`, con validación de igualdad. |
| E | `DetallePago.subConcepto`, bonificación y recargo duplican relaciones del concepto/origen. | Mappers y recibos. | Dos autoridades y cambios retroactivos. | Eliminar; guardar snapshots monetarios/descriptivos necesarios en Cargo. |
| G | `ProcesoEjecutado` intenta representar ejecución de varios schedulers. | `AsistenciaMensualServicio` y generación periódica. | Flag global sin constraint sobre el resultado; carrera multi-instancia. | Eliminar; uniques naturales en mensualidad, matrícula y asistencia. |

## 7. Cobertura exigida por la matriz

Cada decisión canónica se prueba en PostgreSQL 15 mediante:

1. test de catálogo para tablas, tipos, checks, FKs, índices y ausencia de
   cascadas no autorizadas;
2. tests de persistencia y baja lógica para historia;
3. tests de unicidad de períodos y relaciones 1:1 de origen;
4. tests transaccionales/concurrentes de aplicación, reversión y ledgers;
5. MockMvc para mutaciones administrativas/financieras y sus respuestas
   401/403/autorizadas;
6. tests frontend del contrato decimal y flujos de pago/anulación.

No se considera cubierta una relación sólo porque el contexto Spring inicia.
