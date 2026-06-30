# Canonical V1 completion worklog

## Initial state

- Started: 2026-06-30 (America/Buenos_Aires).
- Required branch: `main`.
- Initial HEAD: `8519ef3996b5edb3f16379cf1da99f0b6e36ce7d`.
- Expected remote SHA: `8519ef3996b5edb3f16379cf1da99f0b6e36ce7d`.
- `origin/main` after `git fetch origin main --prune`: `8519ef3996b5edb3f16379cf1da99f0b6e36ce7d`.
- Initial worktree: clean; no unstaged or staged changes.
- Remote: `https://github.com/JerePrograma/le-dance`.
- PostgreSQL rule: Testcontainers PostgreSQL 15 only; no connection to `localhost:5432`.
- Git rule: no branch creation, commit, push, merge, tag, deploy, reset, rebase, restore, clean, or history rewrite.

## Commands executed

| Phase | Command | Result |
| --- | --- | --- |
| Git safety | `git status --short --branch` | PASS: `## main...origin/main` |
| Git safety | `git branch --show-current` | PASS: `main` |
| Git safety | `git rev-parse HEAD` | PASS: expected SHA |
| Git safety | `git log -10 --oneline` | PASS: checkpoint is current HEAD |
| Git safety | `git diff` | PASS: empty |
| Git safety | `git diff --cached` | PASS: empty |
| Git safety | `git rev-parse origin/main` | PASS: expected SHA |
| Git safety | `git remote -v` | PASS: expected GitHub remote |
| Git safety | `git fetch origin main --prune` | PASS: remote reference refreshed |

## Phase status

### Phase 0 - safety and inspection

- Gate: GREEN.
- Applicable repository instructions: root `AGENTS.md`; no more specific `AGENTS.md` files exist.
- Root cause findings: none yet; baseline validation has not started.
- Decisions: preserve the canonical checkpoint; do not restore removed legacy APIs or financial classes.
- Files affected: `docs/refactor/16-canonical-v1-worklog.md`.
- Last coherent unit completed: Git safety verification and worklog initialization.

### Phase 1 - checkpoint baseline

- Gate: RED.
- Production backend compilation: PASS, 277 source files, 21.339 s.
- Backend test compilation: PASS, 12 test source files, 20.511 s.
- Backend `clean verify`: FAIL, 50 tests, 0 failures, 1 error, 0 skipped, 1:08 min.
- Frontend `npm ci`: PASS, 560 packages installed.
- Frontend lint: PASS.
- Frontend Vitest: PASS, 3 files and 8 tests, 4.40 s.
- Frontend TypeScript and Vite build: PASS, 2,275 modules, 11.29 s.
- `status.ps1`: PASS; Docker Desktop 29.3.1 available. Port 5432 was observed as occupied but was not accessed.
- `setup.ps1`: PASS; dependencies only, no services started.
- `validate.ps1`: FAIL because backend verify failed; frontend lint/test/build and local Compose passed.
- Local `docker compose config`: PASS.
- Production Compose with non-sensitive placeholders: PASS.
- Testcontainers: PASS; PostgreSQL `15.12-alpine3.21` used on random host ports.
- Flyway: PASS; exactly one migration validated and applied, schema at V1.
- Hibernate `ddl-auto=validate`: PASS against the isolated V1 database.
- Canonical payment PostgreSQL tests: PASS, 5 tests including concurrent idempotency.
- Security HTTP integration tests: PASS, 12 tests.
- JaCoCo: report/coverage gate NOT REACHED because Surefire failed first.
- Root cause: `docs/refactor/sql/04-financial-inconsistencies.sql` contains an unqualified `alumno_id` in a query that joins two CTEs exposing that column; PostgreSQL reports `column reference "alumno_id" is ambiguous`.
- Baseline changed no implementation files.
- Last coherent unit completed: exact checkpoint baseline captured.

## Errors and root causes

1. Baseline backend error: `DataAuditSqlPostgreSqlTest.ejecutaLasAuditoriasCanonicasComoConsultasDeSoloLectura` fails at line 39 with PostgreSQL error `column reference "alumno_id" is ambiguous` at position 637. The SQL audit predates the canonical query shape and does not qualify the shared column.

## Baseline command results

| Command | Result |
| --- | --- |
| `backend\.\mvnw.cmd -DskipTests compile` | PASS |
| `backend\.\mvnw.cmd -DskipTests test-compile` | PASS |
| `backend\.\mvnw.cmd clean verify` | FAIL: 50 tests, 1 error |
| `frontend\npm ci` | PASS |
| `frontend\npm run lint` | PASS |
| `frontend\npm test -- --run` | PASS: 8/8 |
| `frontend\npm run build` | PASS |
| `scripts\codex\status.ps1` | PASS |
| `scripts\codex\setup.ps1` | PASS |
| `scripts\codex\validate.ps1` | FAIL: backend only |
| `docker compose config` | PASS |
| production Compose with placeholders | PASS |

## Pending work

1. Correct the stale SQL audit and rerun the backend gates.
2. Audit endpoint security, Docker, and CI after convergence is green.
3. Complete information-economy, redundancy, and process-map documents from the implemented model.
4. Audit and simplify persisted information, processes, queries, payloads, DTOs, entities, dependencies, and frontend state where evidence supports a change.
5. Run structural contracts, performance evidence, final gates, Docker builds, and final searches.

## Phase 2 - checkpoint convergence

- Gate: GREEN.
- Change: qualified the credit-ledger columns in `docs/refactor/sql/04-financial-inconsistencies.sql`; no schema or product behavior changed.
- Root cause fixed at the shared audit query instead of weakening `DataAuditSqlPostgreSqlTest`.
- Targeted `backend\.\mvnw.cmd '-Dtest=DataAuditSqlPostgreSqlTest' test`: PASS, 1 test, 40.597 s.
- `backend\.\mvnw.cmd clean verify`: PASS, 50 tests, 0 failures, 0 errors, 0 skipped, 1:01 min.
- Flyway V1, Hibernate validate, Testcontainers PostgreSQL 15.12, financial concurrency/idempotency, security, packaging, and JaCoCo report all completed.
- Frontend gate after the coherent fix: lint PASS; 8/8 tests PASS; TypeScript/Vite build PASS, 2,275 modules, 7.07 s.
- Files affected in this phase: `docs/refactor/sql/04-financial-inconsistencies.sql`, this worklog.
- Last coherent unit completed: checkpoint baseline is fully converged and all pre-optimization code gates are green.

## Phase 3 - demonstrated simplification and contract convergence

- Gate: GREEN.
- Removed five one-implementation/unused service interfaces; retained `IEmailService` because it is an actual external boundary with prod and non-prod implementations.
- Removed empty `EmailControlador` and `MatriculaScheduler` classes.
- Removed unused direct npm dependencies `@faker-js/faker`, `lodash`, `@types/lodash`, and `react-paginate`; transitive lodash packages remain owned by installed dependencies.
- Removed `cargos.updated_at`, which had a default but no update path and therefore did not represent an actual timestamp.
- Replaced entity-side `Instant.now()` in `Pago.createdAt` with Hibernate creation timestamp handling; `PagoServicio` still supplies the configured clock on the business write path.
- Removed the explicit application `flush`; the following aggregate query already triggers JPA AUTO flush in the same transaction.
- Consolidated receipt state: `recibos` now stores only historical document facts (`pago_id`, storage key, generated/sent timestamps); `recibos_pendientes` alone owns technical state, retries and errors.
- Reduced `GET /api/pagos/alumno/{id}` from full payment details to `PagoResumenResponse` (`id`, date, received amount, state). This removes per-payment application, cargo-balance, and credit queries from the list path.
- Aligned frontend stock requests/responses with the canonical backend: decimal price string, no removed date/version fields, and required idempotency key.
- Removed dead navigation links and the full-page reload on every route change.
- Canonical payment/egress screens now use React Query as remote-state authority with centralized query keys; only form/UI state remains local.
- Added a decimal-string regression test that includes a value above JavaScript's safe integer range.
- Introduced failure during this phase: first backend compile failed at `Cargo.java:78` because removing `updatedAt` left its `@UpdateTimestamp/@Column` annotations. Cause was an incomplete mechanical field deletion; both annotations and the import were removed immediately.
- Backend `-DskipTests compile`: PASS after fix, 270 source files, 16.493 s.
- Backend `-DskipTests test-compile`: PASS, 12 test source files, 23.696 s.
- Backend `clean verify`: PASS, 50 tests, 0 failures/errors/skipped, 1:23 min; Flyway exactly V1 and Hibernate validate pass on Testcontainers PostgreSQL 15.12.
- Frontend lint: PASS.
- Frontend tests: PASS, 4 files and 9 tests, 5.43 s.
- Frontend build: PASS, 2,276 modules, 9.89 s.
- Last coherent unit completed: canonical information and frontend/backend contract reductions validated.

## Phase 4 - frontend canonical contract completion

- Gate: GREEN.
- A deliberate decimal-contract pass changed catalog and business monetary fields from TypeScript `number` to decimal `string`.
- Introduced failure: the first `npm run build` after that type change failed in the legacy alumno, inscripción, disciplina, bonificación, concepto, método de pago and recargo forms.
- Root cause: those screens still implemented the removed nested DTO shape and performed monetary arithmetic with browser numbers. The type errors exposed stale frontend behavior rather than a backend incompatibility.
- Resolution: replaced the 1,154-line alumno form, 618-line inscripción form, 286-line inscripción list and 503-line disciplina form with small forms/listing bound to the canonical scalar-ID contracts. Removed frontend tuition/discount totals; the backend remains authoritative for charges and balances.
- Consolidated the discipline contract, removed duplicate TypeScript interfaces, and retained schedule editing with explicit IDs.
- Converted remaining catalog money form values to decimal strings and added the inscripción query key.
- `frontend\npm run lint`: PASS.
- `frontend\npm test -- --run`: PASS, 4 files and 9 tests, 4.08 s.
- `frontend\npm run build`: PASS, 2,272 modules; the focused preceding run completed in 10.20 s.
- Files affected: `frontend/src/types/types.ts`, alumno/inscripción/disciplina forms, inscripción list, catalog forms and `frontend/src/hooks/queryKeys.ts`.
- Last coherent unit completed: frontend DTOs and forms compile against the canonical backend without duplicated monetary calculations.

## Phase 5 - structural contracts, security, performance and CI image gate

- Gate: GREEN for targeted checks; full gates remain pending.
- Added `CanonicalArchitectureContractTest`: exactly one V1 migration; removed financial model tokens stay absent; product time uses `Clock`; no product `printStackTrace`/`@Data`; monetary TypeScript properties cannot regress to `number`.
- The architecture test initially failed because `PdfService` legitimately uses primitive `float[]` for PDF column widths. The contract now permits that one non-monetary file explicitly and still rejects floating point elsewhere.
- A second architecture-test failure found a stale comment suggesting bare `LocalDate.now()`; the comment was corrected to describe the implemented Clock-driven contract. Final targeted result: 2 tests PASS.
- Added a schema assertion that `recibos` cannot regain technical outbox state columns.
- Added an explicit administrator policy group for cargos, pagos, crédito, caja, egresos, stock, matrículas, mensualidades and reportes; all other API endpoints remain administrator-only, and non-API requests are denied.
- `SecurityHttpIntegrationTest`: PASS, 14 tests, 0 skipped, 37.773 s. It now covers the financial endpoint matrix and invalid payment idempotency keys.
- Added deterministic performance test with 500 students and 20,000 charges on Testcontainers PostgreSQL 15.
- Initial query plan: bitmap heap scan plus sort, 39 buffers, 0.174 ms. The first test assertion also used an incorrect literal (`actual rows=32`) and failed despite returning 32 rows.
- Replaced the charge pending index with a partial `(alumno_id, fecha_vencimiento, id)` index. The first repeat still chose bitmap+sort before vacuum visibility and correctly kept the gate red.
- Final measured plan after deterministic `VACUUM ANALYZE`: index-only scan, 0 heap fetches, 6 buffers, 0.064 ms, 32 rows. Targeted performance test PASS in 34.368 s.
- Backend Docker packaging now uses `-DskipTests` only inside BuildKit. CI keeps `clean verify` as the required prior job with Docker/Testcontainers access, then builds SHA-tagged images in a dependent job; no Docker socket mount is used.
- Local development documentation records the validation-before-image contract.
- Last coherent unit completed: enforceable canonical regression contracts and measured pending-charge plan.

## Phase 6 - paginated reads and database-side caja aggregation

- Gate: GREEN.
- Added the stable generic `PageResponse` and a global server maximum of 200 rows per page.
- Paginated the growing alumno, inscripción, pending/overdue cargo, payment-by-student, egreso, stock and caja-movement contracts; corresponding React Query keys include page state.
- Removed local “infinite scroll” that downloaded complete alumno/stock datasets.
- Removed unconsumed duplicate endpoints and code: inscripción bulk, statistics, discipline filter and singular-active route; alumno simplified and graph-style `/{id}/datos`; two DTOs and unused repository/service methods.
- Payment and enrollment forms now request an explicit alumno ID instead of preloading every student.
- Caja totals are now one PostgreSQL aggregate over the ledger; only the requested movement page is hydrated. `PagoCanonicoPostgreSqlTest` executes this native projection after payment reversal.
- Introduced failure: the first backend compile after inscription method removal had duplicate `@Transactional` annotations. Annotation processing stopped, causing many secondary missing-Lombok-method errors. Removing the two orphan annotations restored compilation; no domain code was changed to chase the cascade.
- Introduced frontend failure: the first caja/cargo pagination build passed a click event to a numeric function and kept the old cargo-array query generic. Explicit callback and `Page<CargoResponse>` fixed both.
- Backend `-DskipTests compile`: PASS, 267 production sources, 17.096 s.
- Backend `-DskipTests test-compile`: PASS, 15 test sources, 21.295 s.
- Frontend lint: PASS; 9/9 tests PASS; build PASS, 2,272 modules.
- Backend `clean verify`: PASS, 57 tests, 0 failures/errors/skipped, 1:46 min. Flyway exactly V1, Hibernate validate, schema/data audits, security, finance, concurrency, performance and receipt worker passed on Testcontainers PostgreSQL 15.
- Targeted payment/caja integration test after the native aggregate assertion: PASS, 5 tests, 1:25 min.
- Last coherent unit completed: bounded list payloads and ledger-side caja summary validated end to end.
