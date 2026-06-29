# Fase 2 - Inventario y política de endpoints

Estado del inventario: 2026-06-28.

## Política

La base V1 crea un único rol conocido: `ADMINISTRADOR`. La política implementada evita inventar permisos que el modelo actual no representa:

- `PÚBLICO`: únicamente login, refresh y preflight CORS;
- `AUTENTICADO`: operaciones ordinarias del monolito;
- `ADMINISTRADOR`: gestión de usuarios/roles y descarga de documentos financieros;
- cualquier endpoint nuevo no listado cae en `authenticated()` por defecto.

El frontend no participa de la autorización. Las rutas de usuarios y roles continúan ocultas para otros roles, pero el backend es la autoridad.

## Endpoints públicos

| Método | Ruta | Motivo |
| --- | --- | --- |
| `POST` | `/api/login` | Intercambio de credenciales por tokens. |
| `POST` | `/api/login/refresh` | Renovación usando exclusivamente un refresh token Bearer. |
| `OPTIONS` | `/**` | Preflight CORS; no ejecuta casos de uso. |

`POST /api/usuarios/registro`, `/api/roles` y `/api/pagos/recibo/{id}` dejaron de ser públicos.

## Endpoints de administrador

| Métodos | Rutas | Regla |
| --- | --- | --- |
| `GET` | `/api/usuarios/perfil` | Cualquier usuario autenticado; se evalúa antes del matcher administrativo. |
| `POST` | `/api/usuarios/registro` | `ROLE_ADMINISTRADOR`. |
| `GET`, `PUT`, `DELETE` | `/api/usuarios`, `/api/usuarios/{id}` | `ROLE_ADMINISTRADOR`. La baja es lógica. |
| `GET`, `POST` | `/api/roles`, `/api/roles/{id}` | `ROLE_ADMINISTRADOR`. |
| `GET` | `/api/pagos/recibo/{pagoId}` | `ROLE_ADMINISTRADOR`. |
| `GET` | `/api/pagos/factura/{facturaId}` | `ROLE_ADMINISTRADOR`. |

El sistema no tiene una identidad autenticable de alumno ni una relación usuario-alumno. Por eso se eligió la opción administrativa para recibos y facturas; no se simuló una autorización por propietario inexistente.

## Endpoints autenticados de negocio

Todos los endpoints siguientes exigen un access token válido y un usuario persistido, activo, con rol activo y coherente con el token.

### Alumnos e inscripciones

| Base | Endpoints |
| --- | --- |
| `/api/alumnos` | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `DELETE /dar-baja/{id}`, `GET /listado`, `GET /buscar`, `GET /{alumnoId}/disciplinas`, `GET /{id}/datos` |
| `/api/inscripciones` | `POST /`, `PUT /{id}`, `POST /bulk`, `GET /estadisticas`, `GET /`, `GET /{id}`, `GET /disciplina/{disciplinaId}`, `DELETE /{id}`, `GET /alumno/{alumnoId}`, `GET /alumno/{alumnoId}/activas` |
| `/api/matriculas` | `GET /{alumnoId}`, `PUT /{matriculaId}` |
| `/api/mensualidades` | `POST /`, `GET /{id}`, `GET /inscripcion/{inscripcionId}`, `DELETE /{id}`, `POST /generar-mensualidades` |

### Disciplinas, profesores, salones y asistencia

| Base | Endpoints |
| --- | --- |
| `/api/disciplinas` | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /dar-baja/{id}`, `DELETE /{id}`, `GET /listado`, `GET /por-fecha`, `GET /{disciplinaId}/alumnos`, `GET /{disciplinaId}/profesor`, `GET /por-horario`, `GET /buscar`, `GET /{disciplinaId}/alumnos/pdf` |
| `/api/profesores` | `POST /`, `GET /{id}`, `GET /`, `GET /activos`, `PUT /{id}`, `DELETE /{id}`, `GET /{profesorId}/disciplinas`, `GET /buscar`, `GET /{profesorId}/alumnos` |
| `/api/salones` | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| `/api/asistencias-diarias` | `PUT /registrar`, `PUT /{id}`, `GET /por-asistencia-mensual/{asistenciaMensualId}`, `DELETE /{id}`, `GET /por-disciplina-y-fecha` |
| `/api/asistencias-mensuales` | `GET /`, `POST /`, `PUT /{id}`, `GET /por-disciplina/detalle`, `POST /crear-asistencias-activos-detallado` |
| `/api/observaciones-profesores` | `POST /`, `DELETE /{id}`, `GET /{id}`, `GET /`, `GET /profesor/{profesorId}`, `GET /fechas`, `GET /profesor/{profesorId}/mes` |

### Pagos, caja y configuración comercial

| Base | Endpoints |
| --- | --- |
| `/api/pagos` | `POST /`, `PUT /{id}`, `GET /{id}`, `GET /`, `GET /alumno/{alumnoId}`, `GET /alumno/{alumnoId}/facturas`, `GET /vencidos`, `DELETE /{id}`, `PUT /{id}/quitar-recargo`, `GET /alumno/{alumnoId}/cobranza`, `GET /alumno/{alumnoId}/ultimo`, `GET /filtrar`, `POST /verificar`, `GET /datos-unificados/{alumnoId}` |
| `/api/detalle-pago` | `POST /`, `GET /{id}`, `PUT /{id}`, `PUT /anular/{id}`, `DELETE /{id}`, `GET /fecha`, `GET /alumno/{alumnoId}` |
| `/api/caja` | `GET /planilla`, `GET /dia/{fecha}`, `GET /dia/{fecha}/imprimir`, `GET /mes`, `GET /datos-unificados`, `GET /rendicion/imprimir` |
| `/api/egresos` | `POST /`, `PUT /{id}`, `DELETE /{id}`, `GET /{id}`, `GET /`, `GET /debito`, `GET /efectivo` |
| `/api/metodos-pago` | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `DELETE /baja/{id}` |
| `/api/bonificaciones` | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| `/api/recargos` | `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}` |
| `/api/conceptos` | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET /sub-concepto/{subConceptoDesc}` |
| `/api/sub-conceptos` | `GET /{id}`, `POST /`, `GET /`, `PUT /{id}`, `DELETE /{id}`, `GET /buscar` |
| `/api/stocks` | `POST /`, `GET /`, `GET /activos`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |

### Reportes y notificaciones

| Base | Endpoints |
| --- | --- |
| `/api/reportes` | `POST /mensualidades/exportar`, `GET /mensualidades/buscar`, `GET /mensualidades/buscar-mensualidades-alumno-por-mes` |
| `/api/notificaciones` | `GET /cumpleaneros` |

`DeudaControlador` y `EmailControlador` no publican operaciones HTTP activas en la línea base.

## JWT y estado de usuario

- Cada JWT se verifica una vez y produce `VerifiedToken(subject, userId, role, tokenType, issuedAt, expiresAt)`.
- Access y refresh son tipos cerrados y no intercambiables.
- Firma, expiración, issuer, formato o claims inválidos producen 401 sin detalle interno.
- El filtro vuelve a buscar por `userId` y exige coincidencia de subject y rol.
- Usuario inactivo, rol ausente, rol inactivo o cambio de rol invalidan el token existente.
- Falta de autenticación produce 401; autenticación válida sin rol requerido produce 403.

Cada emisión incluye `jti` y refresh devuelve un par nuevo. El modelo actual no persiste sesiones ni familias de refresh tokens, por lo que todavía no puede detectar de forma durable la reutilización de un refresh anterior entre procesos o reinicios. No se agregó un denylist en memoria porque daría una garantía falsa. La detección durable queda ligada a una migración forward-only y operación atómica en base de datos antes de considerarla completa.

## Bootstrap del primer administrador

El bootstrap está deshabilitado por defecto. Sólo se activa con:

- `APP_BOOTSTRAP_ADMIN_ENABLED=true`;
- `APP_BOOTSTRAP_ADMIN_USERNAME` explícito;
- `APP_BOOTSTRAP_ADMIN_PASSWORD` explícito, entre 12 y 72 bytes UTF-8.

Sólo funciona si no existe ningún usuario y si el rol `ADMINISTRADOR` está activo. Después de crear el usuario, un reinicio con la opción todavía habilitada falla cerrado y exige deshabilitarla. La contraseña se codifica con el `PasswordEncoder`, no se registra y no existe una contraseña fija en código o configuración.

## Riesgos y decisiones pendientes

- No existe todavía un catálogo de permisos de negocio más granular que roles. Crear autoridades nominales sin modelo persistido sería ceremonial y no demostraría aislamiento real.
- La reutilización durable de refresh requiere persistencia y concurrencia PostgreSQL; queda abierta como riesgo explícito para las fases de migraciones/idempotencia.
- Los endpoints de negocio permanecen disponibles para cualquier rol autenticado existente. El único rol creado por las migraciones auditadas es `ADMINISTRADOR`; cualquier rol adicional presente en una base debe inventariarse antes de producción.
