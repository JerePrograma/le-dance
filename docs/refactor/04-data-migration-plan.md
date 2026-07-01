# Plan de baseline de datos

No existe una base productiva ni datos que migrar. El único procedimiento
soportado es crear un esquema vacío y aplicar la baseline canónica V1.

## Verificación

1. Crear PostgreSQL 15 desechable con puerto aleatorio.
2. Ejecutar Flyway y comprobar exactamente una fila exitosa, versión `1`.
3. Arrancar JPA con `ddl-auto=validate`.
4. Ejecutar auditorías de constraints, FKs, tipos monetarios e índices.
5. Ejecutar pruebas financieras, concurrentes, seguridad, outbox y planes.
6. Descartar el contenedor.

No hay upgrade V060->V1, backfill ni reconciliación productiva aplicable. Si se
declara producción en el futuro, V1 queda congelada desde ese momento y toda
evolución será forward-only con una migración nueva y plan de recuperación.

Los SQL de auditoría conservados bajo `docs/refactor/sql` son gates de la baseline
o material histórico; no autorizan conexión a `localhost:5432`.
