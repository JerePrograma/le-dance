package ledance.repositorios;

import ledance.entidades.SubConcepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubConceptoRepositorio extends JpaRepository<SubConcepto, Long> {

    @Query("SELECT s FROM SubConcepto s WHERE LOWER(s.descripcion) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<SubConcepto> buscarPorDescripcion(String descripcion);
}
