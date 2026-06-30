package ledance.repositorios;

import jakarta.persistence.LockModeType;
import ledance.entidades.EstadoReciboPendiente;
import ledance.entidades.ReciboPendiente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReciboPendienteRepositorio extends JpaRepository<ReciboPendiente, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReciboPendiente r where r.estado in :estados and r.nextAttemptAt <= :ahora order by r.id")
    List<ReciboPendiente> findPendientesForUpdate(@Param("estados") List<EstadoReciboPendiente> estados,
                                                  @Param("ahora") Instant ahora,
                                                  Pageable pageable);
}
