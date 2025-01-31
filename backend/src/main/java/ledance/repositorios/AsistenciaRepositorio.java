package ledance.repositorios;

import ledance.entidades.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AsistenciaRepositorio extends JpaRepository<Asistencia, Long> {
    List<Asistencia> findByFechaAndDisciplinaId(LocalDate fecha, Long disciplinaId);
    List<Asistencia> findByAlumnoId(Long alumnoId);
    List<Asistencia> findByDisciplinaId(Long disciplinaId);

    boolean existsByAlumnoIdAndFecha(Long aLong, LocalDate fecha);

    List<Asistencia> findByActivoTrue();

    @Query("SELECT a.alumno.nombre, a.disciplina.nombre, COUNT(a) FROM Asistencia a " +
            "WHERE a.presente = true GROUP BY a.alumno.nombre, a.disciplina.nombre")
    List<Object[]> obtenerAsistenciasPorAlumnoYDisciplina();


    @Query("SELECT a.alumno.nombre, COUNT(a) FROM Asistencia a WHERE a.alumno.id = :alumnoId GROUP BY a.alumno.nombre")
    List<Object[]> obtenerAsistenciasPorAlumno(Long alumnoId);

    @Query("SELECT d.nombre, COUNT(a) FROM Asistencia a JOIN a.disciplina d WHERE d.id = :disciplinaId GROUP BY d.nombre")
    List<Object[]> obtenerAsistenciasPorDisciplina(Long disciplinaId);

    @Query("SELECT d.nombre, a.alumno.nombre, COUNT(a) FROM Asistencia a JOIN a.disciplina d WHERE d.id = :disciplinaId AND a.alumno.id = :alumnoId GROUP BY d.nombre, a.alumno.nombre")
    List<Object[]> obtenerAsistenciasPorDisciplinaYAlumno(Long disciplinaId, Long alumnoId);

}
