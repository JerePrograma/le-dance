# Registro de riesgos

Escala: impacto/probabilidad alta, media o baja. Un riesgo crítico abierto bloquea la fase que lo modifica.

| ID | Riesgo confirmado | Impacto | Prob. | Evidencia | Mitigación/gate |
| --- | --- | --- | --- | --- | --- |
| R01 | Registro público permite elegir rol | Alta | Alta | `POST /api/usuarios/registro` público; request incluye rol | Cerrar endpoint, DTO seguro, bootstrap admin y MockMvc |
| R02 | `/api/roles` público | Alta | Alta | matcher global `permitAll` | ADMIN backend y pruebas 401/403 |
| R03 | Recibos enumerables | Alta | Alta | `GET /api/pagos/recibo/{pagoId}` público | Autorizar por pago/alumno o token firmado temporal |
| R04 | Verificación JWT repetida e inconsistente | Alta | Alta | `getSubject`, `getTokenType`, `getRolFromToken` verifican por separado y envuelven excepciones | Resultado `VerifiedToken`, una verificación y tests |
| R05 | Usuario inactivo puede conservar JWT | Alta | Media | filtro carga usuario pero no rechaza explícitamente `isEnabled=false` | Rechazo 401 y prueba |
| R06 | Frontend borra todo storage y trata 403 como expiración | Alta | Alta | interceptor Axios | Claves propias, 403 conserva sesión, refresh serializado |
| R07 | Perfil común abre como dev | Alta | Alta | `spring.profiles.default=dev` | Eliminar default; context tests dev/test/prod/ausente |
| R08 | PostgreSQL sigue publicado en producción | Alta | Alta | override hereda `ports` de Compose base | Compose productivo autónomo o override inequívoco |
| R09 | Deploy sin healthcheck/rollback/concurrency | Alta | Alta | workflow termina en `compose up -d` | Gate imágenes SHA, salud, timeout, diagnóstico y rollback |
| R10 | Corregida en Fase 3 la mutación de `importeInicial` durante el recálculo; el uso financiero de `Double` sigue abierto | Alta | Media | `MensualidadServicioTest`: saldo, inválidos, sobrepago, fecha y repetición | Mantener `importeInicial` inmutable; migrar tipos sólo con reconciliación posterior |
| R11 | Cerrado en Fase 3: vencidos usa fecha de negocio y consulta sólo ACTIVO, fecha anterior y saldo positivo no nulo | Alta | Baja | `PagoRepositorio.findPagosVencidos` y `PagoServicioTest` con `Clock` fijo | Mantener el predicado en repositorio y el gate de regresión |
| R12 | Mitigado parcialmente: eliminados los `clear()` de serialización y de bajas de alumno/inscripción; quedan seis reemplazos de colecciones clasificados | Alta | Media | inventario de `09-phase-3-financial-p0.md`; `AlumnoServicioTest`, `InscripcionServicioTest`, `PagoServicioTest` | Caracterizar los cuatro reemplazos de horarios y los dos de detalles antes de cambiarlos |
| R13 | Sigue abierto: las bajas tocadas preservan historia, pero continúan `cascade`, `orphanRemoval`, FKs y rutas destructivas fuera de este alcance | Alta | Alta | entidades `Alumno`, `Inscripcion`, `Pago`, `Mensualidad`, `Disciplina` y migraciones existentes | Auditar constraints y rutas restantes; migración compatible antes de cerrar |
| R14 | Pagos parciales mediante clones | Alta | Alta | campos `es_clon`, tipos/resumen | Caracterizar y migrar a aplicaciones |
| R15 | Relaciones financieras inferidas por descripción | Alta | Alta | snapshots y resolvers heredados | IDs explícitos; ambigüedad a reporte, no auto-fix |
| R16 | Sigue abierto: dinero con `Double` y tipos DB mixtos; Fase 3 sólo usa `BigDecimal` localmente para normalizar el saldo | Alta | Alta | entidades/DTOs/cálculos; bigint/numeric/double | Inventario, migración por vertical, reconciliación exacta |
| R17 | V39 fue destructiva | Alta | Desconocida | `DROP TABLE pagos/detalle_pagos CASCADE` | No editar; auditar backup/historial real |
| R18 | V44 borró duplicados automáticamente | Alta | Desconocida | DELETE previo a unique | Auditar efectos históricos; no repetir patrón |
| R19 | V060 usa SQL dinámico amplio | Alta | Media | todas las PK int visibles | Probar V060 y upgrades en copia aislada antes de V061 |
| R20 | Constraints de idempotencia ausentes | Alta | Alta | schedulers + `ProcesoEjecutado` sin unique | Índices únicos y tests concurrentes |
| R21 | Cobertura backend 0,73 % de líneas | Alta | Alta | JaCoCo baseline | Regresión por defecto/caso de uso antes de modificar |
| R22 | Sin tests frontend | Alta | Alta | sin script test | Vitest/RTL mínimo y gate CI |
| R23 | Lint 216 errores/19 warnings | Media | Alta | baseline ESLint | Corregir comportamiento primero; gate cero |
| R24 | 157 endpoints sin matriz explícita de permisos | Alta | Alta | sólo matchers públicos/global auth | Inventario/matriz y method/request security |
| R25 | Dos mecanismos de deploy incompletos | Media | Alta | Compose + PM2 mínimo | Elegir Docker y retirar PM2 salvo evidencia |
| R26 | URL `/api` vs `/api/v1` divergente | Media | Alta | controllers vs Compose prod example | Unificar contrato sin rediseño visual |
| R27 | Instancia desconocida escucha en 5432 | Alta | Media | PID 7164 | No conectar/migrar; usar PostgreSQL desechable aislado |
| R28 | `JAVA_HOME` host inválido | Media | Alta | baseline inicial | Documentar/validar JDK; scripts no instalan herramientas |
| R29 | Efectos PDF/email dentro del flujo financiero | Alta | Media | servicios por caracterizar | after-commit con idempotencia y reintento |
| R30 | Datos personales/logs excesivos | Alta | Media | Lombok `@Data`, logs y `printStackTrace` | Identidad estable, logs por IDs/resultado, pruebas |

## Riesgos de ejecución

- No se autoriza tocar la base que escucha en 5432.
- Toda prueba de migración usará un proyecto/contenedor desechable con nombre y puerto únicos.
- Docker builds pueden consumir tiempo/espacio, pero no deben borrar volúmenes.
- Cambiar todas las áreas en una sola fase impediría atribuir divergencias; cada gate debe quedar verde antes de continuar.
