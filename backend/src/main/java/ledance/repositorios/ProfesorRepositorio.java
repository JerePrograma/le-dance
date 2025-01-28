package ledance.repositorios;

import ledance.entidades.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Busca profesores por su especialidad.
     *
     * @param especialidad La especialidad del profesor.
     * @return Una lista de profesores que coinciden con la especialidad.
     */
    List<Profesor> findByEspecialidad(String especialidad);

    /**
     * Busca profesores por una parte de su nombre o apellido.
     *
     * @param nombre Fragmento del nombre o apellido.
     * @return Una lista de profesores cuyo nombre o apellido contiene el fragmento especificado.
     */
    List<Profesor> findByNombreContainingOrApellidoContaining(String nombre, String apellido);

    /**
     * Busca profesores con mas de un numero especifico de años de experiencia.
     *
     * @param aniosExperiencia Numero minimo de años de experiencia.
     * @return Una lista de profesores con al menos el numero de años de experiencia.
     */
    List<Profesor> findByAniosExperienciaGreaterThanEqual(Integer aniosExperiencia);

    boolean existsByNombreAndApellido(String nombre, String apellido);

    List<Profesor> findByActivoTrue();
}
