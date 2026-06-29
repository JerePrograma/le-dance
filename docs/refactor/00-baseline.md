# Fase 0 - Baseline verificable

Fecha de captura: 2026-06-28 (America/Buenos_Aires).

## Identidad y estado Git

| Dato | Valor |
| --- | --- |
| Repositorio local | `C:\laburo\le-dance` |
| Remoto declarado | `JerePrograma/le-dance` |
| Rama | `main` |
| `HEAD` | `c2da482b73b6843c5b8b535e1c86e5c696e014d6` |
| Línea base auditada | `c2da482b73b6843c5b8b535e1c86e5c696e014d6` |
| Divergencia | Ninguna; `HEAD` coincide con la línea base y no hay commits posteriores. |

Estado local previo, preservado y fuera del alcance de esta fase:

```text
## main...origin/main
 M backend/src/main/java/ledance/controladores/AutenticacionControlador.java
?? .codex/
?? AGENTS.md
```

La modificación previa de `AutenticacionControlador.java` elimina la ejecución de recargos y notificaciones durante el login. No fue revertida ni atribuida a este trabajo. `git diff --cached` estaba vacío.

## Herramientas detectadas

| Herramienta | Resultado |
| --- | --- |
| Git | `2.48.1.windows.1` |
| JDK válido disponible | Amazon Corretto `21.0.7`, `C:\Program Files\Java\corretto-21.0.7` |
| `JAVA_HOME` recibido inicialmente | `C:\Program Files\Eclipse Adoptium\jdk-21`, ruta inválida |
| `java` en `PATH` | Oracle Java `8.0.2510.8`; no es fuente válida para el build |
| Maven Wrapper | Apache Maven `3.9.10` |
| Node.js | `v22.14.0` |
| npm | `10.9.2` |
| Docker CLI / Engine | `29.3.1`, Linux/amd64 |
| Docker Compose | `v5.1.1` |
| PostgreSQL CLI | No instalado en `PATH` |
| PostgreSQL observado | Listener ajeno/no identificado en `5432`, PID 7164; no se accedió a él |
| Spring Boot | `3.4.1` |
| PostgreSQL de Compose | `15.12-alpine3.21` |

El host contiene un JDK 21 válido, pero la variable inicial era incorrecta. Para separar configuración del host de defectos del repositorio, la segunda ejecución usó un override de proceso de `JAVA_HOME`; no se alteró configuración global.

## Dimensión del código actual

| Elemento | Cantidad |
| --- | ---: |
| Java productivo | 259 archivos |
| Tests Java | 4 archivos |
| Entidades JPA | 25 |
| Enums de entidades | 7 |
| Repositorios Spring Data | 25 |
| Controladores | 28 |
| Servicios, procesadores, resolvers y schedulers | 43 |
| DTOs y mappers bajo `ledance.dto` | 109 |
| Mappers | 23 |
| Mappings HTTP declarados | 157 |
| Archivos fuente frontend TS/TSX/CSS | 162 |
| Clientes API frontend | 24 |
| Migraciones Flyway | 60 (`V1` a `V060`) |

## Línea base ejecutada sin correcciones

| Comando | Código | Resultado exacto resumido |
| --- | ---: | --- |
| `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\status.ps1` | 1 | `JAVA_HOME` no contenía el JDK; Maven informó `The JAVA_HOME environment variable is not defined correctly`. Docker estaba disponible. |
| `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\setup.ps1` | 1 | `JAVA_HOME no apunta a un JDK completo: C:\Program Files\Eclipse Adoptium\jdk-21`. |
| `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\codex\validate.ps1` | 1 | `JAVA_HOME no contiene bin\javac.exe`. No llegó a los gates. |
| `cd backend; .\mvnw.cmd clean verify` | 1 | Maven Wrapper rechazó el `JAVA_HOME` inicial antes de compilar. |
| `cd frontend; npm ci` | 0 | `added 476 packages in 26s`. |
| `cd frontend; npm run lint` | 1 | 235 hallazgos: 216 errores y 19 warnings, en 63 archivos. |
| `cd frontend; npm run build` | 0 | Build Vite completo; 2 warnings PostCSS por orden de `@import`. |
| `docker compose config` | 0 | Configuración local válida. Publica DB en 5432, backend en 8080 y frontend en 8081 por default. |
| `docker compose -f docker-compose.yml -f docker-compose.prod.yml config` | 1 | Fallo cerrado por variable faltante: `POSTGRES_USER is required`. |

## Diagnóstico con el JDK 21 existente

Con `JAVA_HOME=C:\Program Files\Java\corretto-21.0.7` limitado al proceso:

| Comando | Código | Resultado |
| --- | ---: | --- |
| `scripts\codex\status.ps1` | 0 | JDK 21.0.7, Wrapper 3.9.10, Node/npm y Docker detectados. |
| `scripts\codex\setup.ps1` | 0 | `dependency:go-offline` terminó `BUILD SUCCESS`; `npm ci` instaló 476 paquetes; no inició servicios. |
| `scripts\codex\validate.ps1` | 1 | Backend PASS; frontend lint FAIL (1); test frontend SKIP por script inexistente; frontend build PASS; Compose local PASS. |
| Backend `clean verify` dentro del gate | 0 | `BUILD SUCCESS`; 17 tests, 0 fallos, 0 errores, 0 omitidos. |

No se accedió a la instancia PostgreSQL que escucha en 5432, no se levantó producción y no se ejecutaron migraciones contra una base persistente.

## Calidad y cobertura actual

JaCoCo después de `clean verify`:

| Métrica | Cubierto | Total | Cobertura |
| --- | ---: | ---: | ---: |
| Instrucciones | 186 | 30.460 | 0,61 % |
| Ramas | 4 | 1.557 | 0,26 % |
| Líneas | 48 | 6.607 | 0,73 % |
| Métodos | 16 | 1.152 | 1,39 % |

El frontend no tiene runner ni script de tests.

Distribución del lint:

| Regla | Hallazgos | Clasificación |
| --- | ---: | --- |
| `@typescript-eslint/no-explicit-any` | 98 | Tipos/contratos |
| `@typescript-eslint/no-unused-vars` | 93 | Variables y errores ignorados |
| `no-empty` | 16 | Flujo y manejo de errores |
| `react-hooks/exhaustive-deps` | 15 | Hooks; posible flujo obsoleto |
| `react-hooks/rules-of-hooks` | 5 | Error real de comportamiento |
| `react-refresh/only-export-components` | 4 | Organización/HMR |
| `@typescript-eslint/no-empty-object-type` | 2 | Tipos |
| `prefer-const` | 2 | Cosmético |

Warnings de compilación backend confirmados:

- propiedades destino no mapeadas en múltiples mappers MapStruct;
- `@Exclude` redundantes de Lombok;
- API deprecada en `MensualidadServicio`;
- operaciones unchecked en `DisciplinaServicio`;
- carga dinámica futura-incompatible del agente Mockito/Byte Buddy.

El build frontend además informa dos warnings PostCSS: los `@import` de `src/index.css` aparecen después de directivas Tailwind.

## Gate de Fase 0

La captura está completa, pero la estabilización no está verde. La Fase 1 debe resolver lint, agregar tests frontend, verificar Compose productivo con valores simulados y conservar el backend verde antes de tocar el modelo financiero.
