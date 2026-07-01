# Implementación de V1 canónica

La implementación vigente retiró el modelo de detalle/clones y lo reemplazó por
Cargo, Pago, AplicacionPago y ledgers de caja/crédito/stock. Los requests usan IDs
explícitos, BigDecimal/NUMERIC, idempotency keys y hashes deterministas.

Los listados de alumnos, inscripciones, cargos pendientes/vencidos, pagos por
alumno, egresos, stock y caja son páginas reales con máximo 200, orden estable e
ID como desempate. Caja agrega en PostgreSQL.

Recibo y ReciboPendiente separan documento histórico de retry técnico. El worker
usa claim PostgreSQL, lease recuperable y transacciones cortas. CI valida el SHA
antes de construir imágenes y no ejecuta deploy.

La evidencia ejecutable reside en los tests `PostgreSqlSchemaValidationTest`,
`CanonicalArchitectureContractTest`, `CanonicalPaginationPostgreSqlTest`,
`CanonicalQueryPlanPostgreSqlTest`, `CajaCanonicaPostgreSqlTest`,
`PagoCanonicoPostgreSqlTest`, `SchedulerIdempotencyPostgreSqlTest`,
`ReciboOutboxPostgreSqlTest` y `SecurityHttpIntegrationTest`.
