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

    // Ejemplo #1: Filtra por alumno, activo y saldoRestante > 0
    @Query("""
           SELECT p
           FROM Pago p
           WHERE p.alumno.id = :alumnoId
             AND p.activo = TRUE
             AND p.saldoRestante > 0
           ORDER BY p.id DESC
           """)
    List<Pago> findPagosPendientesByAlumno(@Param("alumnoId") Long alumnoId);

    // Ejemplo #2 (opcional): Filtra tambien por inscripcionId
    @Query("""
           SELECT p
           FROM Pago p
           WHERE p.inscripcion.id = :inscripcionId
             AND p.activo = TRUE
             AND p.saldoRestante > 0
           ORDER BY p.id DESC
           """)
    List<Pago> findPagosPendientesByInscripcion(@Param("inscripcionId") Long inscripcionId);


/**
     * ✅ Listar pagos por inscripcion, ordenados por fecha descendente.
     */
    List<Pago> findByInscripcionIdOrderByFechaDesc(Long inscripcionId);

    /**
     * ✅ Listar pagos de un alumno (vinculados a sus inscripciones), ordenados por fecha descendente.
     */
    List<Pago> findByInscripcionAlumnoIdOrderByFechaDesc(Long alumnoId);

    /**
     * ✅ Listar solo pagos activos (excluyendo pagos marcados como inactivos).
     */
    List<Pago> findByActivoTrue();

    /**
     * ✅ Obtener pagos vencidos (pagos cuya fecha de vencimiento ya paso).
     */
    @Query("SELECT p FROM Pago p WHERE p.fechaVencimiento < :hoy AND p.activo = true")
    List<Pago> findPagosVencidos(LocalDate hoy);

    List<Pago> findByFechaBetweenAndActivoTrue(LocalDate start, LocalDate end);

    List<Pago> findByFechaAndActivoTrue(LocalDate fecha);

    // Retorna el último Pago (el más reciente) para un alumno, considerando que la relación es a través de la inscripción.
    Optional<Pago> findTopByInscripcionAlumnoIdAndActivoTrueOrderByFechaDesc(Long alumnoId);

}