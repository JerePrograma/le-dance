package ledance.repositorios;

import ledance.entidades.Bonificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BonificacionRepositorio extends JpaRepository<Bonificacion, Long> {

    List<Bonificacion> findByActivoTrue();}
