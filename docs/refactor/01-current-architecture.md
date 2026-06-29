# Arquitectura implementada actual

## Resumen

Le Dance es un monolito Spring Boot con frontend React separado en el mismo repositorio. El backend está organizado principalmente por capas globales (`controladores`, `servicios`, `repositorios`, `entidades`, `dto`), con subcarpetas por tema dentro de servicios y DTOs. No implementa hoy una arquitectura hexagonal completa ni un monolito modular estricto.

```text
React/Vite
    -> Axios (/api)
        -> Spring MVC controllers
            -> application services
                -> Spring Data repositories
                    -> JPA/Hibernate -> PostgreSQL/Flyway
            -> PDF/filesystem and email side effects
```

## Paquetes backend

```text
ledance
├── controladores
├── dto
│   ├── alumno/{request,response}
│   ├── asistencia/{request,response}
│   ├── bonificacion/{request,response}
│   ├── caja/{request,response}
│   ├── cobranza
│   ├── concepto/{request,response}
│   ├── disciplina/{request,response}
│   ├── egreso/{request,response}
│   ├── inscripcion/{request,response}
│   ├── matricula/{request,response}
│   ├── mensualidad/{request,response}
│   ├── metodopago/{request,response}
│   ├── pago/{request,response}
│   ├── profesor/{request,response}
│   ├── recargo/{request,response}
│   ├── reporte/{observacion,request,response}
│   ├── request
│   ├── response
│   ├── rol/{request,response}
│   ├── salon/{request,response}
│   ├── stock/{request,response}
│   └── usuario/{request,response}
├── entidades
├── infra/{configuracion,errores,seguridad}
├── repositorios
├── servicios
│   ├── alumno
│   ├── asistencia
│   ├── bonificacion
│   ├── caja
│   ├── concepto
│   ├── detallepago
│   ├── disciplina
│   ├── egreso
│   ├── email
│   ├── inscripcion
│   ├── matricula
│   ├── mensualidad
│   ├── notificaciones
│   ├── observaciones
│   ├── pago
│   ├── pdfs
│   ├── profesor
│   ├── recargo
│   ├── rol
│   ├── salon
│   ├── stock
│   └── usuario
├── util
└── validaciones/{alumnos,disciplinas,profesores,usuarios}
```

## Inventario backend

Entidades JPA (25): `Alumno`, `AsistenciaAlumnoMensual`, `AsistenciaDiaria`, `AsistenciaMensual`, `Bonificacion`, `Concepto`, `DetallePago`, `Disciplina`, `DisciplinaHorario`, `Egreso`, `Inscripcion`, `Matricula`, `Mensualidad`, `MetodoPago`, `Notificacion`, `ObservacionProfesor`, `Pago`, `ProcesoEjecutado`, `Profesor`, `Recargo`, `Rol`, `Salon`, `Stock`, `SubConcepto`, `Usuario`.

Enums/tipos: `DiaSemana`, `EstadoAsistencia`, `EstadoInscripcion`, `EstadoMensualidad`, `EstadoPago`, `TipoDetallePago`, `TipoPago`.

Repositorios: uno por cada entidad persistida principal; 25 interfaces Spring Data bajo `ledance.repositorios`.

Servicios relevantes:

- alumnos/matrículas: `AlumnoServicio`, `MatriculaServicio`, `MatriculaScheduler` vacío;
- asistencias: `AsistenciaDiariaServicio`, `AsistenciaMensualServicio`;
- facturación/pagos: `MensualidadServicio`, `PagoServicio`, `PaymentProcessor`, `PaymentCalculationServicio`, `DetallePagoServicio`, `DetallePagoResolver`, `MetodoPagoServicio`, `RecargoServicio`, `BonificacionServicio`;
- caja/inventario: `CajaServicio`, `EgresoServicio`, `StockServicio`;
- seguridad: `AutenticacionService`, `TokenService`, `SecurityFilter`, `SecurityConfigurations`;
- efectos externos: `EmailService`, `NoOpEmailService`, `EmailAsyncService`, `PdfService`, `ReciboStorageService`;
- tareas: `ScheduledTasks`.

Existen interfaces `I*Servicio` con una sola implementación para alumno, bonificación, disciplina, inscripción, notificación, profesor, rol y usuario. Sólo el límite de email tiene dos implementaciones con perfiles distintos y representa una frontera externa real.

## API HTTP actual

La aplicación declara 157 mappings de método. Inventario por controlador (base + rutas relativas):

| Base | Rutas |
| --- | --- |
| `/api/alumnos` | `POST /`, `GET /`, `GET/PUT/DELETE /{id}`, `DELETE /dar-baja/{id}`, `GET /listado`, `/buscar`, `/{alumnoId}/disciplinas`, `/{id}/datos` |
| `/api/asistencias-diarias` | `PUT /registrar`, `PUT/DELETE /{id}`, `GET /por-asistencia-mensual/{id}`, `/por-disciplina-y-fecha` |
| `/api/asistencias-mensuales` | `GET/POST /`, `PUT /{id}`, `GET /por-disciplina/detalle`, `POST /crear-asistencias-activos-detallado` |
| `/api/login` | `POST /`, `POST /refresh` |
| `/api/bonificaciones` | CRUD |
| `/api/caja` | `GET /planilla`, `/dia/{fecha}`, `/dia/{fecha}/imprimir`, `/mes`, `/datos-unificados`, `/rendicion/imprimir` |
| `/api/conceptos` | CRUD y `GET /sub-concepto/{descripcion}` |
| `/api/detalle-pago` | `POST /`, `GET/PUT/DELETE /{id}`, `PUT /anular/{id}`, `GET /fecha`, `/alumno/{alumnoId}` |
| `/api/deudas` | Sin endpoint activo |
| `/api/disciplinas` | CRUD/baja y consultas de listado, fecha, alumnos, profesor, horario, búsqueda y PDF |
| `/api/egresos` | CRUD y consultas `/debito`, `/efectivo` |
| `/api/email` | Controlador sin mapping de método visible |
| `/api/inscripciones` | alta/modificación/bulk, estadísticas, listado, detalle, disciplina, baja y consultas por alumno |
| `/api/matriculas` | `GET /{alumnoId}`, `PUT /{matriculaId}` |
| `/api/mensualidades` | alta, detalle, por inscripción, baja y generación masiva |
| `/api/metodos-pago` | CRUD y `DELETE /baja/{id}` |
| `/api/notificaciones` | `GET /cumpleaneros` |
| WebSocket | `NotificacionWSController`, sin ruta REST base |
| `/api/observaciones-profesores` | alta, baja, detalle, listado y consultas por profesor/fecha/mes |
| `/api/pagos` | recibo, CRUD parcial, alumno/facturas/vencidos/cobranza/último, filtros, verificación y datos unificados |
| `/api/profesores` | CRUD, activos, disciplinas, búsqueda y alumnos |
| `/api/recargos` | CRUD |
| `/api/reportes` | exportación y búsquedas de mensualidades |
| `/api/roles` | alta, detalle y listado |
| `/api/salones` | CRUD |
| `/api/stocks` | CRUD y activos |
| `/api/sub-conceptos` | CRUD y búsqueda |
| `/api/usuarios` | registro, modificación, detalle, listado, baja y perfil |

Problemas contractuales confirmados:

- el prefijo backend es `/api`, mientras varias URLs de Compose usan `/api/v1`;
- `GET /api/pagos/recibo/{id}` está público y usa ID secuencial;
- `POST /api/usuarios/registro` y `/api/roles` están públicos;
- no hay autorización explícita por endpoint/rol fuera de autenticación global;
- varios controladores devuelven o manipulan entidades/colecciones para construir respuestas.

## Frontend y contratos

El frontend usa React 18, Vite 6, TypeScript, Formik/Yup, Axios y React Query. La organización es mixta: `funcionalidades/*`, clientes en `api/*`, tipos globales en `types/types.ts` y contexts/hooks globales.

Los 24 clientes API son mantenidos manualmente. No existe OpenAPI ni generación de tipos. Hay 98 usos lintables de `any`, lo que demuestra divergencia o contratos implícitos. `PagosFormulario.tsx` concentra más de 1.500 líneas y contiene hooks invocados dentro de callbacks.

Axios hoy:

- agrega `accessToken` desde `localStorage`;
- trata 403 como sesión expirada, borra todo `localStorage` y redirige;
- hace refresh por cada 401 sin promesa compartida;
- puede dejar la promesa sin rechazo explícito si falla el refresh;
- no excluye de forma robusta la propia llamada de refresh.

## Configuración y despliegue

- configuración Spring: `application.yml`, `application-dev.yml`, `application-test.yml`, `application-prod.yml`;
- fuente de esquema: Flyway `V1` a `V060`; Hibernate usa `ddl-auto=validate` por default;
- ejecución local: scripts PowerShell y Docker Compose;
- despliegue declarado: GitHub Actions + Docker Hub + Compose en VPS;
- mecanismo adicional incompleto: `ecosystem.config.js` para PM2;
- imágenes: backend multi-stage Maven/JRE no root y frontend Node/Nginx;
- persistencia Compose: volúmenes de PostgreSQL y recibos.

El archivo común selecciona `dev` por default. Compose local también usa `dev`. El override productivo exige variables, pero hereda la publicación del puerto PostgreSQL del Compose base y fija `APP_SCHEDULING_ENABLED` en `true`.

## Tareas programadas

`ScheduledTasks` se registra sólo con `app.scheduling-enabled=true` y usa zona configurada:

- mensualidades: día 1 a las 00:00;
- matrículas: 1 de enero a las 00:00;
- recargos: diariamente a la 01:00;
- asistencias: diariamente a las 02:00;
- cumpleaños: diariamente a las 10:00.

La idempotencia depende principalmente de `ProcesoEjecutado`; no hay constraints suficientes para demostrar idempotencia concurrente.

## Dirección incremental

La dirección adecuada es conservar el monolito y mover límites por capacidad sólo cuando cada caso de uso esté caracterizado. No se justifica duplicar entidades JPA/domain, crear interfaces de aplicación sin frontera ni reescribir paquetes en masa.
