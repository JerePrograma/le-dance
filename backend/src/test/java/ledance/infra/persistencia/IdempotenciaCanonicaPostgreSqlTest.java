package ledance.infra.persistencia;

import ledance.dto.credito.request.CreditoAjusteRequest;
import ledance.dto.egreso.request.EgresoAnulacionRequest;
import ledance.dto.egreso.request.EgresoRegistroRequest;
import ledance.dto.stock.request.ReversionStockRequest;
import ledance.dto.stock.request.VentaStockRequest;
import ledance.entidades.Usuario;
import ledance.infra.errores.TratadorDeErrores.OperacionNoPermitidaException;
import ledance.repositorios.UsuarioRepositorio;
import ledance.servicios.credito.CreditoServicio;
import ledance.servicios.egreso.EgresoServicio;
import ledance.servicios.stock.StockServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class IdempotenciaCanonicaPostgreSqlTest extends PostgreSqlIntegrationTest {

    @Autowired private EgresoServicio egresos;
    @Autowired private StockServicio stock;
    @Autowired private CreditoServicio creditos;
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void mismaKeyConcurrenteNoDuplicaYUnPayloadDistintoEntraEnConflicto() throws Exception {
        Fixture fixture = fixture();

        String egresoKey = key("egreso");
        EgresoRegistroRequest egreso = new EgresoRegistroRequest(
                LocalDate.of(2026, 7, 1), "25.00", "insumo", fixture.metodo(), egresoKey);
        List<Long> egresoIds = concurrentes(() -> egresos.agregarEgreso(egreso, fixture.usuario()).id());
        assertThat(egresoIds).containsOnly(egresoIds.getFirst());
        assertThat(count("egresos", egresoKey)).isOne();
        assertThatThrownBy(() -> egresos.agregarEgreso(new EgresoRegistroRequest(
                egreso.fecha(), "26.00", egreso.observaciones(), egreso.metodoPagoId(), egresoKey), fixture.usuario()))
                .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("otro contenido");

        String ventaKey = key("venta");
        VentaStockRequest venta = new VentaStockRequest(
                fixture.alumno(), fixture.stock(), 2, LocalDate.of(2026, 7, 31), ventaKey);
        List<Long> cargoIds = concurrentes(() -> stock.vender(venta, fixture.usuario()).id());
        assertThat(cargoIds).containsOnly(cargoIds.getFirst());
        assertThat(count("ventas_stock", ventaKey)).isOne();
        assertThatThrownBy(() -> stock.vender(new VentaStockRequest(
                fixture.alumno(), fixture.stock(), 3, venta.fechaVencimiento(), ventaKey), fixture.usuario()))
                .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("otro contenido");

        String creditoKey = key("credito");
        CreditoAjusteRequest ajuste = new CreditoAjusteRequest(
                fixture.alumno(), "10.00", "CREDITO", "ajuste auditado", creditoKey);
        List<Long> movimientoIds = concurrentes(() -> creditos.ajustar(ajuste, fixture.usuario()).id());
        assertThat(movimientoIds).containsOnly(movimientoIds.getFirst());
        assertThat(count("movimientos_credito", creditoKey)).isOne();
        assertThatThrownBy(() -> creditos.ajustar(new CreditoAjusteRequest(
                fixture.alumno(), "11.00", "CREDITO", ajuste.motivo(), creditoKey), fixture.usuario()))
                .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("otro contenido");

        String egresoReversalKey = key("egreso-reversal");
        EgresoAnulacionRequest egresoReversal = new EgresoAnulacionRequest(egresoReversalKey, "carga duplicada");
        assertThat(concurrentes(() -> egresos.anular(egresoIds.getFirst(), egresoReversal, fixture.usuario()).id()))
                .containsOnly(egresoIds.getFirst());
        assertThatThrownBy(() -> egresos.anular(egresoIds.getFirst(),
                new EgresoAnulacionRequest(egresoReversalKey, "otro motivo"), fixture.usuario()))
                .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("otro contenido");

        Long ventaId = jdbc.queryForObject("SELECT id FROM ventas_stock WHERE idempotency_key = ?", Long.class, ventaKey);
        String ventaReversalKey = key("venta-reversal");
        ReversionStockRequest ventaReversal = new ReversionStockRequest(ventaReversalKey, "venta incorrecta");
        assertThat(concurrentes(() -> stock.revertirVenta(ventaId, ventaReversal, fixture.usuario()).id()))
                .containsOnly(cargoIds.getFirst());
        assertThatThrownBy(() -> stock.revertirVenta(ventaId,
                new ReversionStockRequest(ventaReversalKey, "otro motivo"), fixture.usuario()))
                .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("otro contenido");
    }

    private List<Long> concurrentes(Callable<Long> operation) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return operation.call(); });
            var second = executor.submit(() -> { start.await(); return operation.call(); });
            start.countDown();
            return List.of(first.get(), second.get());
        }
    }

    private Fixture fixture() {
        String suffix = UUID.randomUUID().toString();
        Long role = jdbc.queryForObject("SELECT id FROM roles WHERE descripcion = 'ADMINISTRADOR'", Long.class);
        Long user = id("""
                INSERT INTO usuarios(nombre_usuario, contrasena, rol_id, activo)
                VALUES (?, 'test-only', ?, true) RETURNING id
                """, "idem-" + suffix, role);
        Long alumno = id("""
                INSERT INTO alumnos(nombre, fecha_incorporacion, activo)
                VALUES (?, DATE '2026-01-01', true) RETURNING id
                """, "Alumno " + suffix);
        Long metodo = id("""
                INSERT INTO metodo_pagos(descripcion, activo, recargo)
                VALUES (?, true, 0) RETURNING id
                """, "Método " + suffix);
        Long stockId = id("""
                INSERT INTO stocks(nombre, precio, cantidad_actual, requiere_control_de_stock, activo)
                VALUES (?, 5, 20, true, true) RETURNING id
                """, "Stock " + suffix);
        return new Fixture(alumno, metodo, stockId, usuarios.findById(user).orElseThrow());
    }

    private int count(String table, String key) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE idempotency_key = ?",
                Integer.class, key);
    }

    private Long id(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        if (value == null) throw new IllegalStateException("La inserción no devolvió id");
        return value;
    }

    private String key(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private record Fixture(Long alumno, Long metodo, Long stock, Usuario usuario) {
    }
}
