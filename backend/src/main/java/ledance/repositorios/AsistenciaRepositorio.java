package ledance.repositorios;

import ledance.entidades.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AsistenciaRepositorio extends JpaRepository<Asistencia, Long> {
    List<Asistencia> findByFechaAndDisciplinaId(LocalDate fecha, Long disciplinaId);
    List<Asistencia> findByAlumnoId(Long alumnoId);
    List<Asistencia> findByDisciplinaId(Long disciplinaId);

    boolean existsByAlumnoIdAndFecha(Long aLong, LocalDate fecha);

    List<Asistencia> findByActivoTrue();
}
