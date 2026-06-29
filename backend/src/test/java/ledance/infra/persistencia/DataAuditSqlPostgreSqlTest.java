package ledance.infra.persistencia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
class DataAuditSqlPostgreSqlTest extends PostgreSqlIntegrationTest {

    private static final List<String> SQL_FILES = List.of(
            "01-counts.sql",
            "02-duplicates.sql",
            "03-orphans.sql",
            "04-financial-inconsistencies.sql",
            "05-state-inconsistencies.sql",
            "06-reconciliation-baseline.sql");

    private static final Pattern MUTATING_SQL = Pattern.compile(
            "(?i)\\b(INSERT|UPDATE|DELETE|MERGE|TRUNCATE|ALTER|DROP|CREATE)\\b");

    @Test
    void ejecutaLasSeisAuditoriasComoConsultasDeSoloLectura() throws Exception {
        Path sqlDirectory = Path.of(System.getProperty("user.dir"), "..", "docs", "refactor", "sql")
                .toAbsolutePath().normalize();
        Map<String, Long> detectedRules = new LinkedHashMap<>();

        try (Connection connection = POSTGRESQL.createConnection("")) {
            seedLegalAnomalies(connection);
            connection.setAutoCommit(true);
            Map<String, Long> countsBefore = tableCounts(connection);

            connection.setReadOnly(true);
            connection.setAutoCommit(false);

            for (String fileName : SQL_FILES) {
                Path file = sqlDirectory.resolve(fileName);
                assertThat(file).isRegularFile();

                String sql = Files.readString(file, StandardCharsets.UTF_8);
                String sqlWithoutComments = sql.replaceAll("(?m)--.*$", "");
                String sqlStructure = sqlWithoutComments.replaceAll("'(?:''|[^'])*'", "''");
                assertThat(MUTATING_SQL.matcher(sqlStructure).find())
                        .as("%s debe ser de solo lectura", fileName)
                        .isFalse();
                assertThat(sqlWithoutComments.lines().noneMatch(line -> line.stripLeading().startsWith("\\")))
                        .as("%s no debe depender de comandos psql", fileName)
                        .isTrue();

                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(sql)) {
                    ResultSetMetaData metadata = result.getMetaData();
                    assertThat(metadata.getColumnCount()).isGreaterThan(1);
                    for (int column = 1; column <= metadata.getColumnCount(); column++) {
                        assertThat(metadata.getColumnLabel(column)).isNotBlank();
                    }

                    int rows = 0;
                    while (result.next()) {
                        rows++;
                        if (fileName.compareTo("02-duplicates.sql") >= 0
                                && fileName.compareTo("05-state-inconsistencies.sql") <= 0) {
                            String ruleId = result.getString("rule_id");
                            assertThat(ruleId).isNotBlank();
                            detectedRules.put(ruleId, result.getLong("affected_count"));
                        }
                    }
                    assertThat(rows).as("resultado tabular de %s", fileName).isPositive();
                }
            }

            assertThat(tableCounts(connection)).isEqualTo(countsBefore);
            connection.rollback();
        }

        assertThat(detectedRules)
                .containsEntry("DUP-INSCRIPCION-ACTIVA", 1L)
                .containsEntry("ORPH-NOTIFICACION-USUARIO", 1L)
                .containsEntry("FIN-PAGO-BALANCE", 1L)
                .containsEntry("STATE-PAGO-ACTIVO-CERO", 1L);
    }

    private void seedLegalAnomalies(Connection connection) throws Exception {
        connection.setAutoCommit(false);
        LocalDate today = LocalDate.of(2026, 6, 29);

        try (Statement statement = connection.createStatement()) {
            long studentId = returningId(statement, """
                    INSERT INTO alumnos (nombre, fecha_incorporacion, activo, credito_acumulado)
                    VALUES ('AUDIT PHASE 4A', DATE '2025-01-01', true, 0)
                    RETURNING id
                    """);
            long disciplineId = returningId(statement, """
                    INSERT INTO disciplinas (nombre, valor_cuota, activo)
                    VALUES ('AUDIT PHASE 4A', 100, true)
                    RETURNING id
                    """);

            statement.executeUpdate("""
                    INSERT INTO inscripciones (alumno_id, disciplina_id, fecha_inscripcion, estado)
                    VALUES (%d, %d, DATE '2026-01-01', 'ACTIVA'),
                           (%d, %d, DATE '2026-02-01', 'ACTIVA')
                    """.formatted(studentId, disciplineId, studentId, disciplineId));
            statement.executeUpdate("""
                    INSERT INTO pagos (
                        fecha, fecha_vencimiento, monto, importe_inicial, alumno_id,
                        saldo_restante, estado_pago, monto_pagado, usuario_id
                    )
                    VALUES (DATE '%s', DATE '%s', 100, 100, %d, 0, 'ACTIVO', 10, NULL)
                    """.formatted(today, today.plusDays(10), studentId));
            statement.executeUpdate("""
                    INSERT INTO notificaciones (usuario_id, tipo, mensaje)
                    VALUES (2147483647, 'AUDIT', 'synthetic phase 4A')
                    """);
        }
        connection.commit();
    }

    private long returningId(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }

    private Map<String, Long> tableCounts(Connection connection) throws Exception {
        Map<String, Long> counts = new LinkedHashMap<>();
        try (Statement tables = connection.createStatement();
             ResultSet result = tables.executeQuery("""
                     SELECT table_name
                     FROM information_schema.tables
                     WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                     ORDER BY table_name
                     """)) {
            while (result.next()) {
                String table = result.getString(1);
                try (Statement count = connection.createStatement();
                     ResultSet rows = count.executeQuery("SELECT COUNT(*) FROM \"" + table + "\"")) {
                    rows.next();
                    counts.put(table, rows.getLong(1));
                }
            }
        }
        return counts;
    }
}
