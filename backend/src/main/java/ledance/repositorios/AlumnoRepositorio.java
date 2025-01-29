package ledance.repositorios;

import ledance.entidades.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AlumnoRepositorio extends JpaRepository<Alumno, Long> {
    boolean existsByNombre(String nombre);

    boolean existsByNombreAndDocumento(String nombre, String documento);

    List<Alumno> findByActivoTrue();

    List<Alumno> findByNombreContainingIgnoreCase(String nombre);

}
