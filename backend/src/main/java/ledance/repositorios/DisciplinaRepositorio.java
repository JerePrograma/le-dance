package ledance.repositorios;

import ledance.entidades.Alumno;
import ledance.entidades.Disciplina;
import ledance.entidades.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisciplinaRepositorio extends JpaRepository<Disciplina, Long> {

    boolean existsByNombre(String nombre);

    List<Disciplina> findByActivoTrue();

    @Query("SELECT I.alumno FROM Inscripcion I WHERE I.disciplina.id = :disciplinaId AND I.alumno.activo = true")
    List<Alumno> findAlumnosPorDisciplina(@Param("disciplinaId") Long disciplinaId);

    @Query("SELECT d.profesor FROM Disciplina d WHERE d.id = :disciplinaId AND d.activo = true")
    Optional<Profesor> findProfesorPorDisciplina(@Param("disciplinaId") Long disciplinaId);

    @Query("SELECT d FROM Disciplina d WHERE LOWER(d.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) AND d.activo = true")
    List<Disciplina> buscarPorNombre(@Param("nombre") String nombre);

    Disciplina findByNombreContainingIgnoreCase(String nombre);

    @Query("SELECT d FROM Disciplina d WHERE d.profesor.id = :profesorId")
    List<Disciplina> findDisciplinasPorProfesor(@Param("profesorId") Long profesorId);

}