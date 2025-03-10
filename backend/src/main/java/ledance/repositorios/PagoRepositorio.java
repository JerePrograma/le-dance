package ledance.repositorios;

import ledance.entidades.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {

    @Query("SELECT p FROM Pago p WHERE p.fechaVencimiento < CURRENT_DATE AND p.saldoRestante > 0")
    List<Pago> findPagosPendientes();

    @Query("""
           SELECT p
           FROM Pago p
           WHERE p.alumno.id = :alumnoId
             AND p.activo = TRUE
             AND p.saldoRestante > 0
           ORDER BY p.id DESC
           """)
    List<Pago> findPagosPendientesByAlumno(@Param("alumnoId") Long alumnoId);

    // Método para listar pagos de un alumno (sin depender de inscripción)
    List<Pago> findByAlumnoIdAndActivoTrueOrderByFechaDesc(Long alumnoId);

    // Método para obtener el último pago para un alumno (sin depender de inscripción)
    Optional<Pago> findTopByAlumnoIdAndActivoTrueOrderByFechaDesc(Long alumnoId);

    // Métodos ya existentes (por inscripcion) se mantienen, si se requieren
    List<Pago> findByInscripcionIdOrderByFechaDesc(Long inscripcionId);

    List<Pago> findByInscripcionAlumnoIdOrderByFechaDesc(Long alumnoId);

    @Query("SELECT p FROM Pago p WHERE p.alumno.id = :alumnoId AND p.saldoRestante > 0 ORDER BY p.fecha DESC")
    List<Pago> findPagosPendientesPorAlumno(@Param("alumnoId") Long alumnoId);

    @Query("SELECT p FROM Pago p WHERE p.fechaVencimiento < :hoy AND p.activo = true")
    List<Pago> findPagosVencidos(LocalDate hoy);

    List<Pago> findByFechaBetweenAndActivoTrue(LocalDate start, LocalDate end);

    List<Pago> findByFechaAndActivoTrue(LocalDate fecha);
}
