package ledance.repositorios;

import ledance.entidades.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlumnoRepositorio extends JpaRepository<Alumno, Long> {

    boolean existsByNombreIgnoreCaseAndApellidoIgnoreCase(String nombre, String apellido);

    // Nuevo método para buscar solo por ID si está activo
    Optional<Alumno> findByIdAndActivoTrue(Long id);

    // Listar todos los activos
    List<Alumno> findByActivoTrue();

    // Buscar por nombre completo **solo** activos
    @Query("""
      SELECT a
        FROM Alumno a
       WHERE a.activo = true
         AND LOWER(CONCAT(a.nombre, ' ', a.apellido)) 
             LIKE LOWER(CONCAT('%', :nombre, '%'))
      """)
    List<Alumno> buscarPorNombreCompleto(@Param("nombre") String nombre);
}
