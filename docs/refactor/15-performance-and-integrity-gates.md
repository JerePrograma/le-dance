# Gates de performance e integridad

| Flujo | Consulta/estrategia | Índice/constraint | Evidencia |
| --- | --- | --- | --- |
| Cargos pendientes | página por alumno/estado/fecha/id | `ix_cargos_alumno_pendientes` parcial | 20.000 cargos sintéticos; index-only, sin seq scan/sort |
| Alumnos | página con filtro y orden estable | `ix_alumnos_activos_nombre` | integración de primera/intermedia/última/vacía |
| Inscripciones | JPQL filtrado en DB + EntityGraph | índices alumno/estado y disciplina/estado | filtro combinado y página real |
| Pagos alumno | resumen proyectado por página | `ix_pagos_alumno_fecha` | sin hidratar aplicaciones en lista |
| Egresos | página fecha/id | `ix_egresos_fecha_metodo` | contrato máximo 200 |
| Stock | página nombre/id | `ix_stocks_activos_nombre` | contrato máximo 200 |
| Caja | agregados `FILTER` + página separada | fecha/método | signos y rango PostgreSQL |
| Schedulers | lock de IDs + lectura batch | uniques de período/origen | dos ejecuciones simultáneas |
| Outbox | `FOR UPDATE SKIP LOCKED` + lease | índice estado/next/lease, unique efecto | dos workers/recuperación |

El plan de cargos se valida por propiedades semánticas: resultado correcto,
ausencia de sequential scan, índice esperado o equivalente y orden correcto. No
hay umbral absoluto de milisegundos.

## Gate de reproducibilidad CI y Docker - 2026-07-01

| Gate | Evidencia |
| --- | --- |
| Backend | `mvnw.cmd clean verify`: PASS; 70 tests, 0 failures, 0 errors, 0 skipped; PostgreSQL 15.12 Testcontainers, Flyway V1, Hibernate validate, JaCoCo y JAR |
| Frontend | `npm ci`, lint, `npm test` y build: PASS; Vitest ejecutó una vez 7 archivos/16 tests y terminó sin modo watch |
| Scripts raíz | `status.ps1`, `setup.ps1` y `validate.ps1`: PASS; setup no inició servicios y validate cerró todos sus gates |
| Compose | Local y productivo con placeholders no sensibles: PASS; producción no publica PostgreSQL ni conserva builds de aplicación |
| Imágenes | Backend y frontend con `--pull`: PASS; tests/Testcontainers fuera de BuildKit, frontend con `npm ci`, runtimes mínimos y sin placeholders sensibles |

Advertencias no bloqueantes: auto-attach de Mockito/Byte Buddy, dialecto
PostgreSQL explícito, `open-in-view` predeterminado, aviso futuro de annotation
processing de `javac`, puerto host 5432 ocupado y nueva versión mayor de npm
disponible. No se corrigieron porque no son fallos de este gate.

Pendiente real: ejecutar el workflow remoto cuando estos cambios tengan un
commit publicado; esta sesión no crea commit ni hace push.

## Gate de aislamiento PostgreSQL y concurrencia - 2026-07-01

El run `28539600117` demostró contaminación entre clases: paginación intentó
borrar alumnos referenciados por cargos y outbox podía dejar un claim esperando
si una aserción ocurría antes de liberar el latch. Los fixtures ahora truncan
sus datos reclamables/dependientes antes del seed y todos los waits, futures y
executors concurrentes tienen cierre acotado. La combinación problemática pasó
11/11 tests y dos `clean verify` consecutivos pasaron 70/70, sin errores,
omitidos ni aumento de `timeout-minutes`.
