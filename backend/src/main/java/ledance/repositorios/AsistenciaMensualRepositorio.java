package ledance.repositorios;

import ledance.entidades.AsistenciaMensual;
import ledance.entidades.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaMensualRepositorio extends JpaRepository<AsistenciaMensual, Long> {

    @Query("SELECT a FROM AsistenciaMensual a WHERE (:profesorId IS NULL OR a.inscripcion.disciplina.profesor.id = :profesorId) " +
            "AND (:disciplinaId IS NULL OR a.inscripcion.disciplina.id = :disciplinaId) " +
            "AND (:mes IS NULL OR a.mes = :mes) AND (:anio IS NULL OR a.anio = :anio)")
    List<AsistenciaMensual> buscarAsistencias(@Param("profesorId") Long profesorId,
                                              @Param("disciplinaId") Long disciplinaId,
                                              @Param("mes") Integer mes,
                                              @Param("anio") Integer anio);

    Optional<AsistenciaMensual> findByInscripcion_IdAndMesAndAnio(Long inscripcionId, Integer mes, Integer anio);

    List<AsistenciaMensual> findByInscripcion_Disciplina_IdAndMesAndAnio(Long disciplinaId, Integer mes, Integer anio);

    Optional<AsistenciaMensual>    findByInscripcionAndMesAndAnio(Inscripcion inscripcion, int monthValue, int year);
}
