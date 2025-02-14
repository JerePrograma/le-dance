package ledance.repositorios;

import ledance.entidades.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PagoRepositorio extends JpaRepository<Pago, Long> {

    /**
     * ✅ Listar pagos por inscripción, ordenados por fecha descendente.
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
     * ✅ Obtener pagos vencidos (pagos cuya fecha de vencimiento ya pasó).
     */
    @Query("SELECT p FROM Pago p WHERE p.fechaVencimiento < :hoy AND p.activo = true")
    List<Pago> findPagosVencidos(LocalDate hoy);
}