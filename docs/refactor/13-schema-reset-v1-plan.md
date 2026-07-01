# Baseline canónica V1

## Estado

El reset pre-productivo está implementado. El directorio Flyway contiene
exactamente `V1__canonical_schema.sql`; no existe V2 ni una ruta de upgrade desde
el modelo retirado.

## Contrato de cambio

Mientras no exista producción, una corrección de modelo modifica en conjunto:

1. V1;
2. entidades/consultas/DTOs;
3. `PostgreSqlSchemaValidationTest` y auditorías;
4. pruebas del caso de uso y concurrencia;
5. documentación de economía, redundancia y procesos.

## Gate de aceptación

- esquema vacío -> Flyway V1;
- exactamente una migración exitosa;
- Hibernate validate;
- tipos monetarios NUMERIC/BigDecimal;
- FKs/constraints/índices auditados;
- finanzas, seguridad, paginación, query plan, schedulers y outbox verdes.

Al declarar la primera producción, V1 se congela. Desde ese punto cualquier
cambio deberá ser una migración forward-only nueva con recuperación y upgrade
probado; esta regla no se anticipa creando una V2 especulativa ahora.
