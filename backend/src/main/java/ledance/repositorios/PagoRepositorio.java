package ledance.repositorios;

import ledance.entidades.EstadoPago;
import ledance.entidades.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {

    @Query("SELECT p FROM Pago p WHERE p.fechaVencimiento < CURRENT_DATE AND p.saldoRestante > 0 AND p.estadoPago <> :estadoAnulado")
    List<Pago> findPagosPendientes(@Param("estadoAnulado") EstadoPago estadoAnulado);

    // Retorna los pagos activos (estadoPago = ACTIVO) de un alumno, con saldo pendiente.
    @Query("SELECT p FROM Pago p WHERE p.alumno.id = :alumnoId AND p.estadoPago = :estadoActivo AND p.saldoRestante > 0 ORDER BY p.id DESC")
    List<Pago> findPagosActivosByAlumno(@Param("alumnoId") Long alumnoId,
                                        @Param("estadoActivo") EstadoPago estadoActivo);

    // Retorna el último pago con el estado indicado (por alumno) ordenado por fecha descendente.
    Optional<Pago> findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(
            Long alumnoId, EstadoPago estadoPago, double saldoRestante);

    // --- Se han eliminado los métodos basados en Inscripcion, ya que la relación Pago → Inscripcion fue removida ---

    // Retorna los pagos vencidos (fechaVencimiento menor a la indicada) y que están activos.
    @Query("SELECT p FROM Pago p WHERE p.fechaVencimiento < :hoy AND p.estadoPago = :estadoActivo")
    List<Pago> findPagosVencidos(@Param("hoy") LocalDate hoy,
                                 @Param("estadoActivo") EstadoPago estadoActivo);

    // Retorna los pagos activos en un rango de fechas.
    List<Pago> findByFechaBetweenAndEstadoPago(LocalDate start, LocalDate end, EstadoPago estadoPago);

    // Retorna los pagos activos en una fecha específica.
    List<Pago> findByFechaAndEstadoPago(LocalDate fecha, EstadoPago estadoPago);

    // Método para listar pagos de un alumno (sin depender de inscripción) que estén activos.
    List<Pago> findByAlumnoIdAndEstadoPagoOrderByFechaDesc(Long alumnoId, EstadoPago estadoPago);

    List<Pago> findByAlumnoIdAndEstadoPagoNotOrderByFechaDesc(Long alumnoId, EstadoPago estado);
}
