# Plan de migración de datos

Este plan no autoriza ejecutar SQL sobre producción. Toda ejecución real requiere entorno inequívoco, backup confirmado, prevalidaciones y autorización explícita.

## Principios

1. V1..V060 permanecen byte a byte intactas.
2. Toda migración nueva es forward-only desde V061.
3. Primero se mide; datos ambiguos se reportan y no se corrigen automáticamente.
4. Se agregan estructuras compatibles antes de cambiar escrituras/lecturas.
5. Las columnas heredadas se conservan hasta reconciliar y operar al menos una ventana acordada.
6. El rollback operativo restaura backup/imágenes previas; no usa down migrations destructivas.

## Precondiciones

- backup lógico y físico verificable;
- checksum e inventario de `flyway_schema_history`;
- conteos por tabla y rango de IDs;
- espacio libre y estimación de locks;
- copia anonimizada representativa en PostgreSQL 15;
- aplicación/schedulers detenidos o ventana de compatibilidad demostrada;
- scripts de reconciliación con resultado esperado explícito.

## Secuencia propuesta

### A. Auditoría de sólo lectura

Ejecutar `docs/refactor/sql/01-counts.sql` a `06-reconciliation-baseline.sql`. Exportar cantidad, IDs, regla, impacto financiero y propuesta. No mutar datos.

### B. Estructuras nuevas

Desde V061, crear tablas objetivo de cargos, aplicaciones, movimientos de caja/stock/crédito y auditoría con PK/FK, `NUMERIC(19,2)` (a confirmar con datos), timestamps y `version`. Agregar índices de FKs. Constraints de negocio que dependan de datos limpios pueden comenzar `NOT VALID`.

### C. Compatibilidad

Agregar IDs de relación explícita a registros heredados cuando la correspondencia sea inequívoca. Mantener columnas antiguas. No rellenar un ID desde descripción salvo para producir un reporte de candidatos sujeto a aprobación.

### D. Backfill determinista

- convertir importes desde numeric/bigint a columnas nuevas con escala explícita;
- crear cargos desde mensualidad, matrícula, concepto y stock sólo cuando el origen sea único;
- crear pagos/aplicaciones desde registros no clonados y relaciones existentes;
- enviar clones, texto ambiguo, saldos negativos y relaciones múltiples a tablas/reportes de excepción;
- registrar conteos antes/después y suma monetaria por alumno/período.

### E. Escritura dual controlada

Cambiar un caso de uso por vez. La transacción escribe modelo nuevo y compatibilidad heredada. Comparar proyecciones, sin usar el frontend como autoridad. Ante divergencia, detener la fase y conservar datos para diagnóstico.

### F. Cambio de lectura

Cambiar consultas después de demostrar reconciliación exacta. Conservar fallback/consulta comparativa sólo durante la ventana acordada, no como abstracción permanente.

### G. Constraints

Después de limpiar casos aprobados: validar FKs/checks, agregar índices únicos parciales e idempotency keys. Probar concurrencia real con PostgreSQL.

### H. Retiro posterior

No eliminar columnas/tablas heredadas en la migración que introduce el modelo. El retiro requiere release posterior, ausencia de lecturas/escrituras, backup y aprobación independiente.

## Reconciliación obligatoria

Por alumno, pago, día y período:

- cantidad e importe original de cargos;
- aplicaciones válidas y revertidas;
- pendiente nuevo frente al heredado;
- dinero recibido frente a caja;
- crédito generado/consumido;
- unidades de stock;
- anulaciones y registros huérfanos.

Toda diferencia se clasifica como explicada y aprobada, o bloquea el cambio de lectura.

## Validación técnica

- base limpia V1..última;
- upgrade desde snapshot exacto V060;
- dataset con duplicados, nulos, clones y ambigüedades;
- Testcontainers/PostgreSQL, no H2;
- locks y duración medidos;
- constraints bajo concurrencia;
- restauración del backup ensayada.
