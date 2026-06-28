# Desarrollo local

## Requisitos

| Herramienta | Versión de referencia | Uso |
| --- | --- | --- |
| Windows PowerShell | 5.1 o PowerShell 7 | Scripts locales y acciones Codex |
| Git | 2.x | Control de versiones |
| JDK | 21 | Compilación y ejecución backend |
| Maven Wrapper | 3.9.10 | Build reproducible; no requiere Maven global |
| Node.js | 22.14.0 | Build frontend |
| npm | 10.x | Instalación reproducible con lockfile |
| Docker Desktop | Engine activo | PostgreSQL y stack en contenedores |
| Docker Compose | v2 o superior | Orquestación local |

No hay componente Python requerido.

## Preparación inicial

1. Definí `JAVA_HOME` con la ruta de un JDK 21. En el host auditado: `C:\Program Files\Java\corretto-21.0.7`.
2. Si vas a usar Compose, creá una configuración local no versionada:

   ```powershell
   Copy-Item .env.local.example .env
   ```

3. Ajustá puertos o credenciales locales en `.env` si difieren de los ejemplos. Compose carga este archivo automáticamente; Maven y Vite usan las variables de la terminal/Codex o sus defaults de desarrollo.
4. Resolvé dependencias sin iniciar servicios ni ejecutar tests completos:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\setup.ps1
   ```

`setup.ps1` exige JDK 21, usa `backend\mvnw.cmd dependency:go-offline` y `frontend\npm ci`. No instala herramientas globales.

## Perfiles Spring

- `dev`: perfil predeterminado; PostgreSQL local, email no-op y schedulers deshabilitados salvo `APP_SCHEDULING_ENABLED=true`.
- `test`: email no-op, schedulers deshabilitados, recibos en temporales y Flyway deshabilitado por defecto. Las pruebas PostgreSQL deben proporcionar su datasource aislado.
- `prod`: datasource, JWT, SMTP/IMAP, CORS, zona horaria y almacenamiento obligatorios; `ddl-auto=validate`; Flyway activo por defecto; email real; schedulers activos.

La configuración común vive en `backend/src/main/resources/application.yml`. Los perfiles no contienen secretos reales.

## Ejecución local

Base de datos:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\start-db.ps1
```

Backend:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\start-backend.ps1
```

Frontend:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\start-frontend.ps1
```

Detener sólo los contenedores del proyecto, conservando volúmenes:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\stop.ps1
```

Los procesos Maven/Vite iniciados en primer plano se detienen desde su propia terminal. Ningún script ejecuta `docker compose down -v`.

## Docker Compose

`docker-compose.yml` es la configuración local: construye imágenes, usa puertos configurables, healthchecks y nombres de proyecto derivados del worktree. Para worktrees paralelos, definí un `COMPOSE_PROJECT_NAME` distinto sólo si los nombres de directorio coinciden.

```powershell
docker compose config
docker compose up -d db
docker compose ps
```

`docker-compose.prod.yml` es un override de despliegue. Exige secretos y URLs explícitos y no debe iniciarse para desarrollo:

```powershell
docker compose -f docker-compose.yml -f docker-compose.prod.yml config
```

Los volúmenes `postgres_data` y `receipts_data` son persistentes. No se eliminan en setup, cleanup ni stop.

## Validación

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\status.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\validate.ps1
```

Validaciones parciales:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\validate.ps1 -Scope Backend
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\validate.ps1 -Scope Frontend
```

El gate completo ejecuta `mvnw.cmd clean verify`, lint, tests frontend sólo si existe el script, build y `docker compose config`. Conserva el primer código de error y muestra todos los resultados.

## Configuración local de Codex

Script de configuración, pestaña Windows:

```powershell
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\setup.ps1
exit $LASTEXITCODE
```

Script de limpieza, pestaña Windows:

```powershell
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\cleanup.ps1
exit $LASTEXITCODE
```

Las variables y acciones completas están en [Variables de entorno](environment-variables.md#variables-recomendadas-para-codex) y en la entrega de esta auditoría.

Acciones recomendadas; todas usan `C:\laburo\le-dance` como directorio de ejecución:

| Nombre | Comando |
| --- | --- |
| Estado | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\status.ps1` |
| Preparar entorno | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\setup.ps1` |
| Validar todo | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\validate.ps1` |
| Validar backend | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\validate.ps1 -Scope Backend` |
| Validar frontend | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\validate.ps1 -Scope Frontend` |
| Iniciar base | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\start-db.ps1` |
| Iniciar backend | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\start-backend.ps1` |
| Iniciar frontend | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\start-frontend.ps1` |
| Ver servicios | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\status.ps1` |
| Detener servicios | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\stop.ps1` |

## Solución de problemas

- `java -version` muestra Java 8 pero existe JDK 21: corregí `JAVA_HOME`; los scripts usan directamente `%JAVA_HOME%\bin\java.exe`.
- Maven informa `JAVA_HOME ... not defined correctly`: la ruta no existe o no contiene `bin\java.exe`.
- Puerto 5432 ocupado: cambiá `POSTGRES_PORT` en `.env`; el backend en Docker sigue usando `db:5432` internamente.
- Docker CLI funciona pero Engine no: iniciá Docker Desktop y esperá a que `docker info` finalice con código 0.
- `npm ci` falla: no reemplaces por `npm install`; verificá que `frontend/package-lock.json` esté presente y sincronizado.
- El backend no valida el esquema: iniciá PostgreSQL, revisá credenciales y no cambies `ddl-auto` a `update`.
- Producción rechaza el inicio: completá todas las variables marcadas como obligatorias; no agregues fallbacks inseguros.
