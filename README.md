# Le Dance

Monorepo de gestión para alumnos, inscripciones, disciplinas, asistencias, mensualidades, pagos, inventario, caja y reportes.

## Stack

- Backend: Java 21, Spring Boot 3.4.1, Maven 3.9.10 Wrapper, PostgreSQL y Flyway.
- Frontend: React 18, TypeScript, Vite 6, Node 22.14.0 y npm.
- Desarrollo local: Windows PowerShell y Docker Compose.

## Inicio rápido en Windows

Requisitos: Git, JDK 21, Node 22.14.0, npm 10 y Docker Desktop con Compose. Configurá `JAVA_HOME` para que apunte al JDK 21.

```powershell
Copy-Item .env.local.example .env
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\setup.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\start-db.ps1
```

`.env` configura únicamente Docker Compose. Para ejecutar Maven o Vite fuera de Compose, exportá las variables en la terminal, el script o el IDE.

En terminales separadas:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\start-backend.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\start-frontend.ps1
```

Validación completa:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\validate.ps1
```

El setup sólo resuelve dependencias. No inicia Docker y no ejecuta la suite completa.

## Documentación

- [Desarrollo local](docs/development/local-development.md)
- [Variables de entorno](docs/development/environment-variables.md)
- [Auditoría del entorno](docs/development/environment-audit.md)

No uses `.env.example` como configuración de producción. Los secretos reales deben permanecer fuera de Git.
