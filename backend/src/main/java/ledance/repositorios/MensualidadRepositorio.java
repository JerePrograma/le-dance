package ledance.repositorios;

import ledance.entidades.EstadoMensualidad;
import ledance.entidades.Inscripcion;
import ledance.entidades.Mensualidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MensualidadRepositorio extends JpaRepository<Mensualidad, Long>, JpaSpecificationExecutor<Mensualidad> {
    List<Mensualidad> findByInscripcionId(Long inscripcionId);

    // Metodo para verificar si ya existe una cuota para una inscripcion en un rango de fechas (mes)
    Optional<Mensualidad> findByInscripcionIdAndFechaGeneracionAndDescripcion(Long inscripcionId,
                                                                              LocalDate fechaGeneracion,
                                                                              String descripcion);

    List<Mensualidad> findByInscripcionAlumnoIdAndEstadoInOrderByFechaCuotaDesc(Long alumnoId, List<EstadoMensualidad> estados);

    Mensualidad findByInscripcionAndDescripcionAndEstado(Inscripcion inscripcion, String descripcion, EstadoMensualidad estado);

    List<Mensualidad> findByInscripcionAlumnoIdAndEstado(Long alumnoId, EstadoMensualidad estado);

    List<Mensualidad> findByDescripcionContainingIgnoreCaseAndEstado(String cuota, EstadoMensualidad estadoMensualidad);

    List<Mensualidad> findByFechaCuotaBeforeAndEstado(LocalDate fecha, EstadoMensualidad estado);

    Optional<Mensualidad> findByInscripcionAlumnoIdAndDescripcionIgnoreCase(Long alumnoId, String descripcion);

    List<Mensualidad> findAllByInscripcionAlumnoIdAndDescripcionAndEsClonFalse(Long alumnoId, String descripcion);

    // Se elimina o comenta este metodo, ya que se basaba en propiedades inexistentes:
    // Optional<Mensualidad> findByInscripcionIdAndMesAndAnio(Long inscripcionId, int mes, int anio);
}
