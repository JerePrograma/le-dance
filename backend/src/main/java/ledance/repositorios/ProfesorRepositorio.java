package ledance.repositorios;

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

    /**
     * Busca profesores por una parte de su nombre o apellido.
     *
     * @param nombre Fragmento del nombre o apellido.
     * @return Una lista de profesores cuyo nombre o apellido contiene el fragmento especificado.
     */
    List<Profesor> findByNombreContainingOrApellidoContaining(String nombre, String apellido);

    boolean existsByNombreAndApellido(String nombre, String apellido);

    List<Profesor> findByActivoTrue();

    @Query("SELECT d FROM Disciplina d WHERE d.profesor.id = :profesorId")
    List<Disciplina> findDisciplinasPorProfesor(@Param("profesorId") Long profesorId);
}