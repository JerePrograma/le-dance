package ledance.repositorios;

import ledance.entidades.Caja;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface CajaRepositorio extends JpaRepository<Caja, Long> {

    Page<Caja> findByFecha(LocalDate fecha, Pageable pageable);

}
