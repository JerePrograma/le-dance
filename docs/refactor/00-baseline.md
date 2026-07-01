# Baseline canónica

Estado reconstruido el 2026-07-01 desde `main` en
`c53f754c1738e7877c7819a701125888797510d4`, inicialmente un commit delante de
`origin/main` (`8519ef3996b5edb3f16379cf1da99f0b6e36ce7d`).

## Runtime soportado

- Java 21, Spring Boot 3.4.1, Maven Wrapper 3.9.10.
- PostgreSQL 15 y Flyway con una sola migración:
  `V1__canonical_schema.sql`.
- React 18, TypeScript, Vite 6, Node 22.14.0 y npm lockfile.
- Docker Compose local y override productivo; CI valida y construye imágenes,
  pero no publica ni despliega.

No existe producción ni una ruta de upgrade desde el modelo retirado V1-V060.
No se debe conectar ni usar como evidencia la instancia en `localhost:5432`.

## Baseline posterior al commit

Antes de modificar, backend compile/test-compile/`clean verify`, frontend
`npm ci`/lint/tests/build, scripts Codex y ambos Compose pasaron. El detalle,
duraciones y puertos aleatorios está en
`docs/refactor/16-canonical-v1-worklog.md`.

## Modelo vigente

La obligación es `Cargo`; un pago recibido es `Pago`; su distribución histórica
es `AplicacionPago`. Caja, crédito y stock son ledgers compensatorios. `Recibo`
guarda hechos documentales y `ReciboPendiente` guarda exclusivamente trabajo
técnico recuperable. No existen clones de deuda, relación financiera por texto
ni saldo de crédito mutable.
