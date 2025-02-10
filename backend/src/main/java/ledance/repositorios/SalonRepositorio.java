package ledance.repositorios;

import ledance.entidades.Salon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalonRepositorio extends JpaRepository<Salon, Long> {
    // Métodos custom si hicieran falta
}
