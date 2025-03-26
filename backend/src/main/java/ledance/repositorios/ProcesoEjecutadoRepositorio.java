package ledance.repositorios;

import ledance.entidades.ProcesoEjecutado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcesoEjecutadoRepositorio extends JpaRepository<ProcesoEjecutado, Long> {
    Optional<ProcesoEjecutado> findByProceso(String proceso);
}
