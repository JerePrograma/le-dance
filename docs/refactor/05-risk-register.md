# Registro de riesgos

Escala: impacto/probabilidad alta, media o baja. Un riesgo crítico abierto bloquea la fase que lo modifica.

| ID | Estado | Riesgo confirmado | Impacto | Prob. | Evidencia actual | Mitigación/gate pendiente |
| --- | --- | --- | --- | --- | --- | --- |
| R01 | CERRADO | Registro público permitía elegir rol | Alta | Baja | Fase 2: `/api/usuarios/**` exige ADMINISTRADOR; MockMvc cubre 401/403 y registro administrativo | Mantener pruebas de autorización |
| R02 | CERRADO | `/api/roles` era público | Alta | Baja | Fase 2: `/api/roles/**` exige ADMINISTRADOR; MockMvc cubre acceso denegado/permitido | Mantener pruebas de autorización |
| R03 | CERRADO | Recibos eran públicos y enumerables | Alta | Baja | Fase 2: `/api/pagos/recibo/**` exige ADMINISTRADOR y tiene pruebas 401/403 | Rediseñar ownership sólo si aparece identidad de alumno |
| R04 | CERRADO | Verificación JWT repetida e inconsistente | Alta | Baja | `TokenService.verify` produce `VerifiedToken`; access/refresh se separan y prueban | Mantener una sola ruta de verificación |
| R05 | CERRADO | Usuario inactivo podía conservar JWT | Alta | Baja | Fase 2: filtro rechaza usuario inactivo; integración HTTP cubre 401 | Mantener prueba al cambiar sesión/revocación |
| R06 | CERRADO | Frontend borraba todo storage y trataba 403 como expiración | Alta | Baja | Axios elimina sólo claves propias, preserva sesión en 403 y serializa refresh; 8 tests frontend | Mantener gate Vitest |
| R07 | CERRADO | Perfil común abría como dev | Alta | Baja | `ActiveProfileGuard` y tests de dev/test/prod/ausente; no existe default dev común | Mantener perfiles explícitos |
| R08 | CERRADO | PostgreSQL quedaba publicado en producción | Alta | Baja | Compose productivo elimina `db.ports`; ambos Compose validan fail-closed | Mantener validación de configuración |
| R09 | MITIGADO | Deploy carecía de healthcheck, rollback y concurrency | Alta | Media | Fase 1 agregó imágenes SHA, healthchecks, timeout, diagnóstico, rollback y concurrency | Verificar rollback real en staging autorizado |
| R10 | MITIGADO | `importeInicial` se mutaba al recalcular; el dinero sigue en `Double` | Alta | Media | Fase 3 preserva el original y prueba saldo, inválidos, sobrepago, fecha y repetición | Migrar tipos sólo después de auditoría y reconciliación autorizadas |
| R11 | CERRADO | Vencidos podía usar estado/predicado incorrecto | Alta | Baja | `PagoRepositorioPostgreSqlTest` ejecuta JPQL real en PostgreSQL 15: sólo ACTIVO, fecha anterior y saldo positivo | Mantener el test PostgreSQL en `clean verify` |
| R12 | MITIGADO | `clear()` y reemplazos de colecciones pueden borrar historial | Alta | Media | Fase 3 eliminó los `clear()` de serialización y bajas de alumno/inscripción; quedan seis reemplazos clasificados | Caracterizar cuatro reemplazos de horarios y dos de detalles |
| R13 | ABIERTO | Cascadas, `orphanRemoval`, FKs y rutas físicas aún pueden destruir historia | Alta | Alta | Entidades y V060 conservan relaciones destructivas; Fase 4A no las cambia | Auditar rutas y diseñar migración compatible |
| R14 | ABIERTO | Pagos parciales mediante clones | Alta | Alta | `es_clon` en mensualidades/detalles y flujo heredado; SQL `FIN-DETALLE-CLON`/`FIN-PARCIAL-MEDIANTE-COPIA` | Requiere snapshot y modelo de aplicaciones posterior |
| R15 | ABIERTO | Relaciones financieras inferidas por descripción | Alta | Alta | `DetallePagoResolver` y búsquedas heredadas por descripción siguen activos | IDs explícitos; ambigüedad a reporte, no auto-fix |
| R16 | ABIERTO | Dinero con `Double` y tipos DB mixtos | Alta | Alta | Hibernate validate falla en `alumnos.credito_acumulado`: BIGINT V060 frente a `Double`; también existen tipos bigint/numeric heredados | Resolver en migración financiera con snapshot y reconciliación; no falsear validate |
| R17 | REQUIERE DATOS | V39 fue destructiva | Alta | Desconocida | V39 elimina y recrea pagos/detalles; una base limpia no revela pérdida histórica | Requiere backup/snapshot autorizado y evidencia operativa |
| R18 | REQUIERE DATOS | V44 borró duplicados automáticamente | Alta | Desconocida | DELETE previo a unique permanece inmutable; una base limpia no mide su efecto | Requiere snapshot/historial autorizado |
| R19 | MITIGADO | V060 usa SQL dinámico amplio | Alta | Media | Testcontainers aplicó V1–V060 en PostgreSQL 15.12 y Flyway validó 60 checksums; configuración de esquema ajena puede autobloquear V060 | Probar upgrade de snapshot V060 autorizado antes de V061 |
| R20 | ABIERTO | Constraints de idempotencia ausentes | Alta | Alta | `procesos_ejecutados` no tiene unique; SQL `DUP-PROCESO-EJECUTADO` y `STATE-SCHEDULER-SIN-REGISTROS` | Constraints y pruebas concurrentes en fase posterior |
| R21 | ABIERTO | Cobertura backend insuficiente | Alta | Alta | JaCoCo actual: 742/6667 líneas, 11,13 %, sobre 57 tests verdes excluyendo sólo el gate JPA/Flyway bloqueado; no hay gate porcentual global | Agregar regresión por defecto/caso de uso antes de modificarlo |
| R22 | CERRADO | No existían tests frontend | Alta | Baja | Vitest ejecuta 3 archivos y 8 tests en el gate normal | Mantener tests al tocar flujos críticos |
| R23 | CERRADO | Lint tenía 216 errores y 19 warnings | Media | Baja | `npm run lint` actual termina con 0 errores y 0 warnings | Mantener gate ESLint |
| R24 | ABIERTO | Endpoints sin matriz explícita completa de permisos | Alta | Alta | Matchers críticos están endurecidos, pero no existe matriz revisada de todos los controladores | Inventario y pruebas por capacidad |
| R25 | CERRADO | Existían dos mecanismos de deploy incompletos | Media | Baja | Fase 1 retiró PM2; Docker Compose es el único mecanismo soportado | Mantener una sola ruta de deploy |
| R26 | CERRADO | URL `/api` vs `/api/v1` divergía | Media | Baja | Controladores y default Compose/frontend usan `/api` | Mantener un único contrato configurable |
| R27 | MITIGADO | Instancia desconocida escucha en localhost:5432 | Alta | Media | Fase 4A usa exclusivamente Testcontainers con puerto aleatorio y prueba que no sea 5432; no se conectó a la instancia | No ejecutar auditorías fuera de snapshot autorizado |
| R28 | MITIGADO | `JAVA_HOME` heredado es inválido | Media | Alta | Baseline inicial falló; scripts pasan con `C:\Program Files\Java\corretto-21.0.7` y validan `javac` 21 | Corregir configuración del host fuera del repositorio |
| R29 | ABIERTO | PDF/email siguen dentro del flujo financiero | Alta | Media | `PagoServicio` invoca generación/almacenamiento/envío en el caso de uso | After-commit idempotente con reintento, cubierto por tests |
| R30 | ABIERTO | Datos personales y logs excesivos | Alta | Media | Persisten entidades `@Data`, logs de requests/cálculos y `printStackTrace` heredados | Reducir por flujo con pruebas; no hacer barrido masivo |

## Riesgos de ejecución

- No se autoriza tocar la base que escucha en 5432.
- Toda prueba de migración usará un proyecto/contenedor desechable con nombre y puerto únicos.
- Docker builds pueden consumir tiempo/espacio, pero no deben borrar volúmenes.
- Cambiar todas las áreas en una sola fase impediría atribuir divergencias; cada gate debe quedar verde antes de continuar.
