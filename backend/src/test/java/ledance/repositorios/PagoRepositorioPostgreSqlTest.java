package ledance.repositorios;

import ledance.entidades.EstadoPago;
import ledance.infra.persistencia.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
@Transactional
class PagoRepositorioPostgreSqlTest extends PostgreSqlIntegrationTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 29);

    @Autowired
    private PagoRepositorio pagoRepositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void findPagosVencidosRespetaFechaEstadoYSaldoEnPostgreSql() throws SQLException {
        Long alumnoId = jdbc.queryForObject("""
                INSERT INTO alumnos (nombre, fecha_incorporacion, activo, credito_acumulado)
                VALUES ('TEST PHASE 4A', ?, true, 0)
                RETURNING id
                """, Long.class, HOY.minusYears(1));

        Long vencidoActivo = insertarPago(alumnoId, HOY.minusDays(1), 100, EstadoPago.ACTIVO);
        insertarPago(alumnoId, HOY, 100, EstadoPago.ACTIVO);
        insertarPago(alumnoId, HOY.plusDays(1), 100, EstadoPago.ACTIVO);
        insertarPago(alumnoId, HOY.minusDays(1), 0, EstadoPago.ACTIVO);
        insertarPago(alumnoId, HOY.minusDays(1), -1, EstadoPago.ACTIVO);
        insertarPago(alumnoId, HOY.minusDays(1), 100, EstadoPago.HISTORICO);
        insertarPago(alumnoId, HOY.minusDays(1), 100, EstadoPago.ANULADO);

        assertThat(pagoRepositorio.findPagosVencidos(HOY, EstadoPago.ACTIVO))
                .extracting("id")
                .containsExactlyInAnyOrder(vencidoActivo);

        assertNotNullConstraint("saldo_restante", HOY.minusDays(1), null);
        assertNotNullConstraint("fecha_vencimiento", null, 100);
    }

    private Long insertarPago(Long alumnoId, LocalDate vencimiento, double saldo, EstadoPago estado) {
        return jdbc.queryForObject("""
                INSERT INTO pagos (
                    fecha, fecha_vencimiento, monto, importe_inicial, alumno_id,
                    saldo_restante, estado_pago, monto_pagado, usuario_id
                )
                VALUES (?, ?, 100, 100, ?, ?, ?, 0, NULL)
                RETURNING id
                """, Long.class, HOY.minusDays(10), vencimiento, alumnoId, saldo, estado.name());
    }

    private void assertNotNullConstraint(String column, LocalDate vencimiento, Integer saldo) throws SQLException {
        String sql = """
                INSERT INTO pagos (
                    fecha, fecha_vencimiento, monto, importe_inicial, alumno_id,
                    saldo_restante, estado_pago, monto_pagado, usuario_id
                )
                VALUES (?, ?, 100, 100, ?, ?, 'ACTIVO', 0, NULL)
                """;

        try (Connection connection = POSTGRESQL.createConnection("")) {
            connection.setAutoCommit(false);
            Long alumnoId;
            try (PreparedStatement seed = connection.prepareStatement("""
                    INSERT INTO alumnos (nombre, fecha_incorporacion, activo, credito_acumulado)
                    VALUES ('TEST CONSTRAINT PHASE 4A', ?, true, 0)
                    RETURNING id
                    """)) {
                seed.setObject(1, HOY.minusYears(1));
                try (var result = seed.executeQuery()) {
                    result.next();
                    alumnoId = result.getLong(1);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, HOY.minusDays(10));
                statement.setObject(2, vencimiento);
                statement.setObject(3, alumnoId);
                statement.setObject(4, saldo);

                assertThatThrownBy(statement::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                                .as("constraint NOT NULL de %s", column)
                                .isEqualTo("23502"));
            }
            connection.rollback();
        }
    }
}
