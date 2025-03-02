package ledance.repositorios;

import ledance.entidades.SubConcepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubConceptoRepositorio extends JpaRepository<SubConcepto, Long> {
    // Metodos adicionales si fueran necesarios
}
