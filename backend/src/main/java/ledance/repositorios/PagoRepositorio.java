package ledance.repositorios;

import jakarta.persistence.LockModeType;
import ledance.entidades.EstadoPago;
import ledance.entidades.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {
    Optional<Pago> findByIdempotencyKey(String idempotencyKey);
    Optional<Pago> findByReversalIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id")
    Optional<Pago> findByIdForUpdate(@Param("id") Long id);

    List<Pago> findByAlumnoIdOrderByFechaDescIdDesc(Long alumnoId);
    List<Pago> findByFechaBetweenAndEstadoOrderByFechaAscIdAsc(LocalDate desde, LocalDate hasta, EstadoPago estado);
}
