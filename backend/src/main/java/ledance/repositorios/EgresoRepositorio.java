package ledance.repositorios;

import ledance.entidades.Egreso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EgresoRepositorio extends JpaRepository<Egreso, Long> {

    // Filtrar egresos por fecha exacta
    List<Egreso> findByFecha(LocalDate fecha);

    // Filtrar egresos en un rango de fechas
    List<Egreso> findByFechaBetween(LocalDate start, LocalDate end);
}
