package ledance.repositorios;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ledance.entidades.Notificacion;

@Repository
public interface NotificacionRepositorio extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByTipoAndFechaCreacion(@NotNull String tipo, @NotNull LocalDateTime fechaCreacion);
}
