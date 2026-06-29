# Plantilla de ejecución de auditoría de datos

Estado de esta Fase 4A: no existe snapshot anonimizado autorizado y no se ejecutó ninguna auditoría sobre datos reales.

## Autorización y trazabilidad

| Campo | Valor |
| --- | --- |
| Identificador de ejecución | `[completar]` |
| Responsable de ejecución | `[completar]` |
| Fecha/hora y zona | `[completar]` |
| Origen del snapshot | `[completar]` |
| Propietario/autorizante del origen | `[completar]` |
| Evidencia de autorización | `[completar]` |
| Método de anonimización | `[completar]` |
| Responsable de anonimización | `[completar]` |
| Verificación de anonimización | `[completar]` |
| Archivo/imagen restaurada | `[completar]` |
| Algoritmo de checksum | `[completar]` |
| Checksum del snapshot | `[completar]` |
| PostgreSQL | `[completar; objetivo 15.x]` |
| Versión Flyway inicial/final | `[completar]` |
| Host/puerto aislado | `[completar; nunca localhost:5432]` |
| Contenedor/proyecto efímero | `[completar]` |
| Evidencia de modo read-only | `[completar]` |

## Gates previos

- [ ] El snapshot está anonimizado y autorizado por escrito.
- [ ] El checksum fue calculado antes de restaurar.
- [ ] El destino es efímero, aislado y no publica el puerto 5432 del host.
- [ ] No se reutilizan credenciales ni URLs de ambientes externos.
- [ ] Flyway informa la versión esperada y checksums válidos.
- [ ] Los seis SQL coinciden con la revisión aprobada.
- [ ] La sesión que ejecuta las auditorías está en modo read-only.

## Conteos e inventario

| Resultado | Valor |
| --- | --- |
| Filas totales inventariadas | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Tablas con filas | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Registros activos | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Registros inactivos | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Claves relevantes nulas | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Migraciones Flyway exitosas | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Migraciones Flyway fallidas | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |

Adjuntar la salida completa de `01-counts.sql`; no copiar conteos manualmente sin conservar el artefacto original.

## Hallazgos

| Archivo | Reglas con hallazgos | Filas/grupos afectados | Evidencia adjunta |
| --- | --- | --- | --- |
| `02-duplicates.sql` | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | `[completar]` |
| `03-orphans.sql` | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | `[completar]` |
| `04-financial-inconsistencies.sql` | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | `[completar]` |
| `05-state-inconsistencies.sql` | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | `[completar]` |

Por cada `rule_id` con `affected_count > 0`, registrar clasificación, IDs de ejemplo acotados, impacto y reparabilidad sin inferir una corrección.

## Reconciliación financiera

| Dimensión | Filas/grupos | Diferencia balance | Diferencia detalles | Diferencia caja |
| --- | --- | --- | --- | --- |
| Alumno | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Fecha | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Período | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Pago | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Mensualidad | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |
| Método de pago | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO | NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO |

Adjuntar la salida completa de `06-reconciliation-baseline.sql`. Conservar separadas las fuentes contradictorias; una diferencia no autoriza a elegir una como verdad.

## Decisiones

| ID | Hallazgo/regla | Decisión | Responsable | Fecha | Evidencia/aprobación |
| --- | --- | --- | --- | --- | --- |
| `[completar]` | `[completar]` | `[completar]` | `[completar]` | `[completar]` | `[completar]` |

Las opciones válidas son: reparar inequívocamente en una migración futura, requerir decisión de negocio, conservar como excepción aprobada o bloquear la migración. Esta plantilla no autoriza ninguna escritura.

## Cierre y aprobación

| Rol | Nombre | Resultado | Fecha | Firma/evidencia |
| --- | --- | --- | --- | --- |
| Dueño de datos | `[completar]` | `[APROBADO / RECHAZADO]` | `[completar]` | `[completar]` |
| Responsable financiero | `[completar]` | `[APROBADO / RECHAZADO]` | `[completar]` | `[completar]` |
| Responsable técnico | `[completar]` | `[APROBADO / RECHAZADO]` | `[completar]` | `[completar]` |
| Seguridad/privacidad | `[completar]` | `[APROBADO / RECHAZADO]` | `[completar]` | `[completar]` |

Resultado global actual: **NO EJECUTADO — REQUIERE SNAPSHOT ANONIMIZADO AUTORIZADO**.
