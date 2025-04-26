package ledance.repositorios;

import ledance.entidades.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DetallePagoRepositorio extends JpaRepository<DetallePago, Long>, JpaSpecificationExecutor<DetallePago> {
    List<DetallePago> findByAlumnoIdAndImportePendienteGreaterThan(Long alumnoId, double valor);

    @Query("SELECT CASE WHEN COUNT(dp) > 0 THEN true ELSE false END FROM DetallePago dp WHERE dp.matricula.id = :id")
    boolean existsByMatriculaId(@Param("id") Long id);

    boolean existsByMensualidadId(Long id);

    @Query("""
  SELECT d 
    FROM DetallePago d 
   WHERE d.matricula.id = :matriculaId 
     AND d.estadoPago = 'ACTIVO'
""")
    Optional<DetallePago> findActiveByMatriculaId(@Param("matriculaId") Long matriculaId);

    boolean existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(Long alumnoId, String descripcion, TipoDetallePago tipo);

    List<DetallePago> findByFechaRegistroBetween(
            LocalDate fechaDesde, LocalDate fechaHasta);

    boolean existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(Long alumnoId, String descripcion, TipoDetallePago tipoDetallePago, EstadoPago estadoPago);

    List<DetallePago> findAllByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(Long alumnoId, String descripcion, TipoDetallePago tipoDetallePago);

    boolean existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPagoNot(Long alumnoId, String descripcion, TipoDetallePago tipoDetallePago, EstadoPago estadoPago);

    Optional<DetallePago> findTopByMensualidadOrderByFechaRegistroDesc(Mensualidad mensualidad);

    Optional<DetallePago> findByMensualidadAndEstadoPago(Mensualidad mensualidad, EstadoPago estadoPago);

    List<DetallePago> findByAlumnoIdAndDescripcionConceptoAndEstadoPago(
            Long alumnoId,
            String descripcionConcepto,
            EstadoPago estadoPago
    );

    long countByPagoIdAndEstadoPagoNot(Long pagoId, EstadoPago estado);

    long countByPagoId(Long pagoId);

    @Modifying
    @Query("UPDATE DetallePago d SET d.usuario = null WHERE d.usuario.id = :uid")
    void clearUsuarioFromDetallePagos(@Param("uid") Long usuarioId);
}
