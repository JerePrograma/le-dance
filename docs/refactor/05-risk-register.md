# Registro de riesgos

Evaluación 2026-07-01. `CERRADO` exige implementación, constraint cuando aplica,
prueba y gate verde; `MITIGADO` conserva una limitación explícita.

| ID | Estado | Riesgo | Evidencia actual |
| --- | --- | --- | --- |
| R01 | CERRADO | Registro público con rol elegible | catch-all administrador + `SecurityHttpIntegrationTest` |
| R02 | CERRADO | Roles públicos | política y 401/403 probados |
| R03 | CERRADO | Recibos públicos/enumerables | recibo administrador; anónimo 401, rol incorrecto 403 |
| R04 | CERRADO | Verificación JWT divergente | `TokenService.verify`, tipos access/refresh y tests |
| R05 | CERRADO | JWT de usuario inactivo | filtro vuelve a leer usuario; test 401 |
| R06 | CERRADO | 403 borraba sesión/refresh concurrente | interceptor y tests frontend |
| R07 | CERRADO | Perfil dev implícito | perfiles explícitos y `RuntimeProfilesTest` |
| R08 | CERRADO | PostgreSQL publicado en producción | override productivo elimina puertos; config validada |
| R09 | CERRADO | Deploy automático o no verificable | workflow actual sólo valida/construye; no push/deploy |
| R10 | CERRADO | Monto original mutable/dinero flotante | Cargo inmutable, BigDecimal/NUMERIC y contrato estructural |
| R11 | CERRADO | Cargos vencidos incorrectos/lentos | consulta canónica + plan PostgreSQL determinista |
| R12 | CERRADO | `clear()` para serializar/borrar historia | DTOs; búsqueda contractual; no clear financiero |
| R13 | CERRADO | Cascadas destruyen historia | FKs financieras RESTRICT; schema test; única cascada de horarios |
| R14 | CERRADO | Pagos parciales por clones | AplicacionPago; tokens de clon prohibidos por arquitectura |
| R15 | CERRADO | Relación financiera por descripción | IDs/FKs explícitos; descripciones sólo snapshot |
| R16 | CERRADO | Dinero Double/Float | contrato Java/TS + PostgreSQL NUMERIC |
| R17 | NO APLICA | Pérdida histórica de antigua V39 | no existe producción ni upgrade heredado soportado |
| R18 | NO APLICA | Borrado histórico de antigua V44 | misma razón; única baseline V1 |
| R19 | NO APLICA | SQL dinámico de antigua V060 | V060 no está en runtime; exactamente una migración probada |
| R20 | CERRADO | Idempotencia incompleta | keys/constraints/hashes; concurrencia probada en pago, egreso, stock, crédito, schedulers y outbox |
| R21 | MITIGADO | Cobertura insuficiente | suites críticas PostgreSQL/HTTP/frontend verdes; no existe umbral global de porcentaje |
| R22 | CERRADO | Sin tests frontend | Vitest forma parte de gate/CI |
| R23 | CERRADO | Lint rojo | ESLint sin warnings en gate actual |
| R24 | CERRADO | Endpoints sin política completa | públicos explícitos, perfil autenticado y catch-all admin; inventario 07 |
| R25 | CERRADO | Dos mecanismos de deploy | runtime documentado en Compose; CI no despliega |
| R26 | CERRADO | `/api` y `/api/v1` divergentes | contrato único `/api` configurable |
| R27 | MITIGADO | Instancia desconocida en 5432 | tests sólo Testcontainers/puertos aleatorios; prohibición documentada |
| R28 | MITIGADO | `JAVA_HOME` del host | gates usan Corretto 21.0.7 por proceso; configuración global queda fuera del repo |
| R29 | MITIGADO | PDF/email dentro de pago | outbox con transacciones cortas, lease y retry; ver R31 |
| R30 | MITIGADO | Logs con datos personales | se retiraron username, destinatario/subject y email de errores; falta auditoría de operación en producción |
| R31 | ABIERTO | Crash después de aceptación SMTP y antes de `enviado_at` | SMTP actual no acepta clave idempotente verificable; no puede prometerse exactly-once externo |

R31 no compromete atomicidad del pago ni duplica procesamiento concurrente. Para
cerrarlo se necesita un proveedor con idempotency key/consulta de entrega o un
protocolo de confirmación externo; una bandera local no alcanza.
