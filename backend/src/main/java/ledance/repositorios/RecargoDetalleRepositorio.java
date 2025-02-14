package ledance.repositorios;

import ledance.entidades.RecargoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecargoDetalleRepositorio extends JpaRepository<RecargoDetalle, Long> {

    // Para listar los detalles de un recargo concreto
    List<RecargoDetalle> findAllByRecargoId(Long recargoId);

    // Para eliminar todos los detalles de un recargo antes de actualizar
    @Query("delete from RecargoDetalle d where d.recargo.id = :recargoId")
    void deleteAllByRecargoId(Long recargoId);
}