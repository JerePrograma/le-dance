package ledance.repositorios;

import ledance.entidades.Mensualidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MensualidadRepositorio extends JpaRepository<Mensualidad, Long>, JpaSpecificationExecutor<Mensualidad> {
    List<Mensualidad> findByInscripcionId(Long inscripcionId);
}
