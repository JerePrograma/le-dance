package ledance.repositorios;

import ledance.entidades.EstadoMensualidad;
import ledance.entidades.Inscripcion;
import ledance.entidades.Mensualidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MensualidadRepositorio extends JpaRepository<Mensualidad, Long>, JpaSpecificationExecutor<Mensualidad> {
    List<Mensualidad> findByInscripcionId(Long inscripcionId);

    // Método para verificar si ya existe una cuota para una inscripción en un rango de fechas (mes)
    Optional<Mensualidad> findByInscripcionIdAndFechaCuotaBetween(Long inscripcionId, LocalDate inicio, LocalDate fin);

    Mensualidad findByInscripcionAndDescripcionAndEstado(Inscripcion inscripcion, String descripcion, EstadoMensualidad estado);

}
