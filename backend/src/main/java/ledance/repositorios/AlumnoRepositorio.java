package ledance.repositorios;

import ledance.dto.response.AlumnoListadoResponse;
import ledance.entidades.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlumnoRepositorio extends JpaRepository<Alumno, Long> {
    boolean existsByNombre(String nombre);

    boolean existsByNombreAndDocumento(String nombre, String documento);

    List<Alumno> findByActivoTrue();

    @Query("SELECT Alumno(a.id, a.nombre, a.apellido) " +
            "FROM Alumno a WHERE CONCAT(a.nombre, ' ', a.apellido) LIKE %:nombre%")
    List<Alumno> buscarPorNombreCompleto(@Param("nombre") String nombre);

}
