package ledance.repositorios;

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

    @Query("SELECT a FROM Alumno a WHERE LOWER(CONCAT(a.nombre, ' ', a.apellido)) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Alumno> buscarPorNombreCompleto(@Param("nombre") String nombre);

    @Query("SELECT a FROM Alumno a WHERE a.deudaPendiente = true")
    List<Alumno> listarAlumnosConDeuda();
}