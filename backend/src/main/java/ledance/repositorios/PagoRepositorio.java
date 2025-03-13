package ledance.repositorios;

import ledance.entidades.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {

    // Retorna todos los pagos que tienen saldo pendiente, sin importar el alumno.
    @Query("SELECT p FROM Pago p WHERE p.fechaVencimiento < CURRENT_DATE AND p.saldoRestante > 0")
    List<Pago> findPagosPendientes();

    // Retorna los pagos activos (activo = true) de un alumno, con saldo pendiente.
    @Query("""
           SELECT p
           FROM Pago p
           WHERE p.alumno.id = :alumnoId
             AND p.activo = TRUE
             AND p.saldoRestante > 0
           ORDER BY p.id DESC
           """)
    List<Pago> findPagosActivosByAlumno(@Param("alumnoId") Long alumnoId);

    // Retorna el último pago activo (por alumno) ordenado por fecha descendente.
    Optional<Pago> findTopByAlumnoIdAndActivoTrueOrderByFechaDesc(Long alumnoId);

    // Retorna los pagos asociados a una inscripción, ordenados por fecha descendente.
    List<Pago> findByInscripcionIdOrderByFechaDesc(Long inscripcionId);

    // Retorna los pagos de un alumno considerando la inscripción, ordenados por fecha descendente.
    List<Pago> findByInscripcionAlumnoIdOrderByFechaDesc(Long alumnoId);

    // Retorna los pagos pendientes de un alumno, filtrando por saldo pendiente, ordenados por fecha.
    @Query("SELECT p FROM Pago p WHERE p.alumno.id = :alumnoId AND p.saldoRestante > 0 ORDER BY p.fecha DESC")
    List<Pago> findPagosPendientesPorAlumno(@Param("alumnoId") Long alumnoId);

    // Retorna los pagos vencidos (fechaVencimiento menor a la fecha indicada) y que están activos.
    @Query("SELECT p FROM Pago p WHERE p.fechaVencimiento < :hoy AND p.activo = true")
    List<Pago> findPagosVencidos(@Param("hoy") LocalDate hoy);

    // Retorna los pagos activos en un rango de fechas.
    List<Pago> findByFechaBetweenAndActivoTrue(LocalDate start, LocalDate end);

    // Retorna los pagos activos en una fecha específica.
    List<Pago> findByFechaAndActivoTrue(LocalDate fecha);

    // Método para listar pagos de un alumno (sin depender de inscripción)
    List<Pago> findByAlumnoIdAndActivoTrueOrderByFechaDesc(Long alumnoId);

    Optional<Pago> findTopByAlumnoIdAndActivoTrueAndSaldoRestanteGreaterThanOrderByFechaDesc(Long alumnoId, Double saldoMinimo);
}
