# Esquema actual reconstruido

Fuente: lectura estática completa de las migraciones Flyway `V1` a `V060` y contraste con las entidades JPA. No se consultó la base local desconocida en 5432. Por lo tanto, este documento describe el esquema esperado por el historial, no certifica el contenido de un entorno real.

## Historial Flyway

- 60 migraciones versionadas, máxima versión 60.
- `V1` crea un esquema amplio y datos iniciales.
- `V2` a `V38` evolucionan pagos, mensualidades, asistencias y catálogos.
- `V39` elimina y recrea físicamente `pagos` y `detalle_pagos`; su ejecución histórica pudo perder datos.
- `V44` elimina matrículas duplicadas antes de agregar `UNIQUE (alumno_id, anio)`.
- `V52` agrega `usuario_id DEFAULT 1` a pagos y detalles.
- `V060` convierte dinámicamente todas las PK/FK simples `INTEGER` a `BIGINT` en los esquemas visibles, salvo el historial Flyway.

Estas migraciones se consideran aplicadas e inmutables. Todo cambio futuro comienza en `V061`.

## Tablas esperadas

| Tabla | Propósito | Claves/constraints relevantes |
| --- | --- | --- |
| `roles` | roles de seguridad | PK bigint; descripción única; activo |
| `usuarios` | credenciales/rol | usuario único; FK rol con RESTRICT; activo |
| `stocks` | stock actual | precio numeric; stock `>= 0`; código sin unicidad; activo |
| `recargos` | regla de recargo | día 1..31; porcentaje/valor fijo no negativos |
| `profesores` | profesores | usuario opcional único; activo |
| `salones` | salones | sin unicidad por nombre |
| `disciplinas` | oferta académica | FK profesor/salón; valores monetarios numeric; activo |
| `disciplina_horarios` | horarios | FK disciplina con cascade; duración double precision |
| `disciplina_dias` | tabla heredada de días | PK compuesta disciplina/día; aparentemente sin entidad actual |
| `bonificaciones` | descuentos | porcentaje entero y valor fijo numeric; activo |
| `alumnos` | estudiantes | datos personales; crédito bigint desde V46; activo/baja |
| `inscripciones` | alumno-disciplina | FKs con `ON DELETE CASCADE`; estado; sin unicidad de activa |
| `asistencias_mensuales` | planilla disciplina/mes | FK disciplina; sin unicidad disciplina/mes/año |
| `asistencias_alumno_mensual` | alumno-inscripción en planilla | FKs inscripción/planilla; sin unicidad |
| `asistencias_diarias` | asistencia diaria | FK al registro alumno-mes; sin unicidad de fecha |
| `observaciones_mensuales` | tabla heredada | FKs a asistencia mensual y alumno; no tiene entidad JPA actual |
| `metodo_pagos` | medios de pago | recargo numeric; activo |
| `pagos` | cabecera de cobro | alumno requerido; método/usuario opcionales; estado varchar; montos numeric salvo `valor_base bigint`; sin `@Version` |
| `detalle_pagos` | líneas de pago/deuda | FKs de origen; versión nullable; uniques alumno-matrícula y alumno-mensualidad sólo declarados en JPA tras recreación V39, no agregados por migración posterior |
| `pago_medios` | tabla heredada de medios por pago | FKs con cascade; sin entidad actual |
| `caja` | totales diarios heredados | totales numeric no negativos; sin entidad actual |
| `egresos` | egresos | monto numeric; método opcional; activo |
| `sub_conceptos` | catálogo | sin unicidad |
| `conceptos` | catálogo cobrable | precio numeric; FK sub-concepto RESTRICT |
| `matriculas` | matrícula anual | `UNIQUE (alumno_id, anio)`; FK alumno |
| `mensualidades` | deuda mensual | FK inscripción con cascade; importes mezclan numeric/bigint; permite clones |
| `procesos_ejecutados` | marca de scheduler | sin unique por proceso/fecha |
| `observaciones_profesores` | notas | FK profesor |
| `notificaciones` | notificaciones | `usuario_id` sin FK; timestamp; leída |
| `reportes` | reportes heredados | FK usuario con cascade; sin entidad actual |

Tablas eliminadas por migraciones: `recargo_detalles`, `tipo_stocks`. La tabla `observacion_mensual` singular se elimina condicionalmente; `observaciones_mensuales` plural de V1 no se elimina.

## Relaciones destructivas

El historial o mapeo actual permite eliminación en cascada de datos históricos:

- alumno -> inscripciones y matrículas por `cascade=ALL` + `orphanRemoval=true`;
- disciplina -> inscripciones/horarios por `cascade=ALL` + `orphanRemoval=true`;
- inscripción -> mensualidades/asistencias por `cascade=ALL` + `orphanRemoval=true`;
- pago -> detalles por `cascade=ALL` + `orphanRemoval=true`;
- mensualidad -> detalles por `cascade=ALL` + `orphanRemoval=true`;
- asistencias mensual/alumno -> detalle diario con cascades;
- varias FKs de V1 usan `ON DELETE CASCADE` para alumno, inscripción, pago y reportes.

Hay llamadas `clear()` activas sobre colecciones administradas en `AlumnoServicio`, `InscripcionServicio`, `PagoServicio`, `PaymentProcessor` y servicios de disciplina. Algunas son eliminación explícita de horarios, pero otras pueden borrar historial o se usan para serialización.

## Campos monetarios y tipos

El modelo Java usa `Double`/`double` de extremo a extremo. PostgreSQL usa principalmente `NUMERIC`, pero también existen incompatibilidades heredadas:

- `pagos.valor_base BIGINT`;
- `mensualidades.importe_pendiente BIGINT` desde V38;
- `alumnos.credito_acumulado BIGINT` desde V46;
- `disciplina_horarios.duracion DOUBLE PRECISION` (no monetario, pero flotante);
- V29 introdujo temporalmente `monto_base_pago DOUBLE PRECISION`, luego renombrado.

Campos monetarios principales: cuota total/crédito de alumno; precios y stock; valores de disciplina; recargos/bonificaciones; mensualidad (`valor_base`, `importe_inicial`, `importe_pendiente`, `monto_abonado`); pago (`monto`, `valor_base`, `importe_inicial`, `monto_pagado`, `saldo_restante`); detalles; egresos; medios y totales de caja.

## Constraints e índices existentes

Confirmados en migraciones:

- uniques: rol/descripción, usuario/nombre, profesor/usuario, matrícula/alumno-año;
- checks: stock no negativo, estados iniciales, días/meses/años, diversos montos no negativos;
- índices puntuales: observaciones, asistencias, pagos/inscripción histórico, reportes/usuario, disciplina/día, detalles y medios.

Faltan o no están demostrados:

- inscripción activa única alumno-disciplina;
- mensualidad única inscripción-período;
- asistencia mensual/diaria única;
- código de barras único condicionado;
- checks consistentes entre estado y saldo;
- igualdad `monto_pagado + saldo_restante = importe_inicial`;
- índices para todas las FKs actuales;
- idempotency keys y versiones optimistas en agregados principales;
- FK de notificación a usuario;
- referencia de origen exactamente una en detalles.

## Divergencias JPA/Flyway a verificar en PostgreSQL aislado

- `DetallePago.pago` declara `nullable=false`, pero V40 permite NULL y usa `ON DELETE SET NULL`.
- los `@UniqueConstraint` de `DetallePago` pudieron desaparecer con V39 y no fueron recreados por SQL.
- la longitud y nulabilidad de varias columnas dependen de defaults Hibernate no expresados en las migraciones.
- enums Java se almacenan como varchar sin checks completos tras V39.
- entidades ausentes para `caja`, `pago_medios`, `reportes`, `observaciones_mensuales` y `disciplina_dias`.
- V060 opera sobre todas las tablas/esquemas visibles y requiere validación en una copia representativa.

La certificación final requiere aplicar V1..V060 en PostgreSQL limpio y restaurar una copia anonimizada en V060 para comparar el catálogo y ejecutar reconciliación.
