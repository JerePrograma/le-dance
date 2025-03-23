package ledance.repositorios;

import ledance.entidades.DetallePago;
import ledance.entidades.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DetallePagoRepositorio extends JpaRepository<DetallePago, Long>, JpaSpecificationExecutor<DetallePago> {
    List<DetallePago> findByAlumnoIdAndImportePendienteGreaterThan(Long alumnoId, double valor);

    @Query("SELECT dp FROM DetallePago dp WHERE dp.id = :id")
    DetallePago buscarPorIdJPQL(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(dp) > 0 THEN true ELSE false END FROM DetallePago dp WHERE dp.matricula.id = :id")
    boolean existsByMatriculaId(@Param("id") Long id);

    Optional<DetallePago> findByMensualidadIdAndAlumnoIdAndCobradoFalse(Long id, Long alumnoId);

    Optional<DetallePago> findByMatriculaIdAndAlumnoIdAndCobradoFalse(Long id, Long alumnoId);

    Optional<DetallePago> findByDescripcionConceptoAndAlumnoIdAndCobradoFalse(String upperCase, Long alumnoId);

    boolean existsByMensualidadId(Long id);
}
