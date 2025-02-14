package ledance.repositorios;

import ledance.entidades.Recargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecargoRepositorio extends JpaRepository<Recargo, Long> {
}