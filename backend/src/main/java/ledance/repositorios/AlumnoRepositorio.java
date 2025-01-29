package ledance.repositorios;

import ledance.entidades.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AlumnoRepositorio extends JpaRepository<Alumno, Long> {
    boolean existsByNombre(String nombre);

    boolean existsByNombreAndDocumento(String nombre, String documento);

    List<Alumno> findByActivoTrue();

    // Consulta personalizada para buscar por nombre + apellido concatenados
    @Query("SELECT a FROM Alumno a WHERE CONCAT(a.nombre, ' ', a.apellido) LIKE %:nombre%")
    List<Alumno> buscarPorNombreCompleto(@Param("nombre") String nombre);

}
