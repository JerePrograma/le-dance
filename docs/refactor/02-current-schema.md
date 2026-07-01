# Esquema canónico actual

Fuente de verdad: `backend/src/main/resources/db/migration/V1__canonical_schema.sql`.
Es la única migración activa y se valida desde cero con PostgreSQL 15.

## Grupos de tablas

| Grupo | Tablas principales | Integridad relevante |
| --- | --- | --- |
| Identidad | `roles`, `usuarios` | usuario normalizado único, FK role RESTRICT |
| Alumnos/clases | `alumnos`, `profesores`, `salones`, `disciplinas`, `disciplina_horarios`, `inscripciones` | bajas lógicas, inscripción activa única |
| Facturación | `mensualidades`, `matriculas`, `cargos` | período único; un único origen de cargo |
| Pagos | `pagos`, `aplicaciones_pago` | NUMERIC, key+hash, aplicación única pago/cargo |
| Caja/crédito | `movimientos_caja`, `movimientos_credito`, `egresos` | ledger inmutable, reverso único, keys únicas |
| Stock | `stocks`, `ventas_stock`, `movimientos_stock` | cantidad no negativa, venta/reverso idempotentes |
| Recibos | `recibos`, `recibos_pendientes` | un recibo y un efecto por pago; claim/lease coherentes |
| Asistencia | `asistencias_mensuales`, `asistencias_alumno_mensual`, `asistencias_diarias` | períodos/fechas únicas |

Todas las FKs históricas financieras usan `ON DELETE RESTRICT`. La única cascada
SQL es `disciplina_horarios`, una colección de configuración reemplazable; no
alcanza pagos, cargos, aplicaciones, inscripciones, asistencias ni ledgers.

No existen columnas `Double/Float`, `detalle_pagos`, `es_clon`, crédito acumulado
mutable ni timestamps `updated_at` ornamentales. Los request hashes son SHA-256
hexadecimales de 64 caracteres y los decimales usan `NUMERIC(19,2)`.
