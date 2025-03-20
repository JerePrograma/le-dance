package ledance.repositorios;

import ledance.entidades.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepositorio extends JpaRepository<Matricula, Long> {
    List<Matricula> findByAlumnoIdAndAnio(Long alumnoId, Integer anio);

    Optional<Matricula> findFirstByAlumnoIdAndAnioOrderByIdAsc(Long alumnoId, int anio);

    // Devuelve la primera matrícula pendiente (no pagada) para el alumno
    Optional<Matricula> findFirstByAlumnoIdAndPagadaFalseOrderByIdAsc(Long alumnoId);

    List<Matricula> findByAlumnoIdAndPagadaFalse(Long alumnoId);

    // Busca una matrícula para un alumno en un año determinado que aún no esté pagada.
    Optional<Matricula> findByAlumnoIdAndAnioAndPagadaFalse(Long alumnoId, int anio);
}