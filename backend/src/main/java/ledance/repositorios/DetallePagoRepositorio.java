package ledance.repositorios;

import ledance.entidades.DetallePago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetallePagoRepositorio extends JpaRepository<DetallePago, Long>, JpaSpecificationExecutor<DetallePago> {
    List<DetallePago> findByAlumnoIdAndImportePendienteGreaterThan(Long alumnoId, double valor);

    boolean existsByMatriculaId(Long matriculaId);

    @Query("SELECT dp FROM DetallePago dp WHERE dp.id = :id")
    DetallePago buscarPorIdJPQL(@Param("id") Long id);

}
