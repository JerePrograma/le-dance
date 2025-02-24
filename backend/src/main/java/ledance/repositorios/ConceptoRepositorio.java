package ledance.repositorios;

import ledance.entidades.Concepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptoRepositorio extends JpaRepository<Concepto, Long> {
    // Puedes agregar métodos de consulta adicionales si lo requieres
}
