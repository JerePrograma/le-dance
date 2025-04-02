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

    // Retorna el ultimo pago con el estado indicado (por alumno) ordenado por fecha descendente.
    Optional<Pago> findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(
            Long alumnoId, EstadoPago estadoPago, double saldoRestante);

    // --- Se han eliminado los metodos basados en Inscripcion, ya que la relacion Pago → Inscripcion fue removida ---

    // Retorna los pagos vencidos (fechaVencimiento menor a la indicada) y que estan activos.
    @Query("SELECT p FROM Pago p WHERE p.fechaVencimiento < :hoy AND p.estadoPago = :estadoActivo")
    List<Pago> findPagosVencidos(@Param("hoy") LocalDate hoy,
                                 @Param("estadoActivo") EstadoPago estadoActivo);

    // Retorna los pagos activos en una fecha especifica.
    List<Pago> findByFechaAndEstadoPago(LocalDate fecha, EstadoPago estadoPago);

    // Metodo para listar pagos de un alumno (sin depender de inscripcion) que esten activos.
    List<Pago> findByAlumnoIdAndEstadoPagoOrderByFechaDesc(Long alumnoId, EstadoPago estadoPago);

    List<Pago> findByAlumnoIdAndEstadoPagoNotOrderByFechaDesc(Long alumnoId, EstadoPago estado);

    List<Pago> findByAlumnoId(Long id);

    List<Pago> findByFechaBetween(LocalDate start, LocalDate end);
}
