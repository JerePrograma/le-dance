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
