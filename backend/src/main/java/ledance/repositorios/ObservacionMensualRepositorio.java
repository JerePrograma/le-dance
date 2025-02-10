package ledance.repositorios;

import ledance.entidades.ObservacionMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObservacionMensualRepositorio extends JpaRepository<ObservacionMensual, Long> {
    // Add custom query methods if needed
}