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
public interface ProfesorRepositorio extends JpaRepository<Profesor, Long> {

    /**
     * Busca un profesor por su usuario asociado.
     *
     * @param usuarioId ID del usuario asociado.
     * @return Un Optional que contiene al profesor si existe.
     */
    Optional<Profesor> findByUsuarioId(Long usuarioId);

    boolean existsByNombreAndApellido(String nombre, String apellido);

    List<Profesor> findByActivoTrue();

    @Query("SELECT d FROM Disciplina d WHERE d.profesor.id = :profesorId")
    List<Disciplina> findDisciplinasPorProfesor(@Param("profesorId") Long profesorId);

    @Query("SELECT p FROM Profesor p WHERE LOWER(CONCAT(p.nombre, ' ', p.apellido)) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Profesor> buscarPorNombreCompleto(@Param("nombre") String nombre);

}