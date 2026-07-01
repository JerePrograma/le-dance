package ledance.servicios.egreso;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.egreso.request.EgresoAnulacionRequest;
import ledance.dto.egreso.request.EgresoRegistroRequest;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.entidades.Egreso;
import ledance.entidades.EstadoPago;
import ledance.entidades.MetodoPago;
import ledance.entidades.MovimientoCaja;
import ledance.entidades.TipoMovimientoCaja;
import ledance.entidades.Usuario;
import ledance.infra.errores.TratadorDeErrores.OperacionNoPermitidaException;
import ledance.infra.idempotencia.RequestHash;
import ledance.repositorios.EgresoRepositorio;
import ledance.repositorios.MetodoPagoRepositorio;
import ledance.repositorios.MovimientoCajaRepositorio;
import ledance.repositorios.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class EgresoServicio {
    private static final Logger log = LoggerFactory.getLogger(EgresoServicio.class);
    private final EgresoRepositorio egresos;
    private final MetodoPagoRepositorio metodos;
    private final UsuarioRepositorio usuarios;
    private final MovimientoCajaRepositorio caja;
    private final Clock clock;

    public EgresoServicio(EgresoRepositorio egresos, MetodoPagoRepositorio metodos,
                          UsuarioRepositorio usuarios, MovimientoCajaRepositorio caja, Clock clock) {
        this.egresos = egresos;
        this.metodos = metodos;
        this.usuarios = usuarios;
        this.caja = caja;
        this.clock = clock;
    }

    @Transactional
    public EgresoResponse agregarEgreso(EgresoRegistroRequest request, Usuario principal) {
        String hash = hash(request);
        Egreso previo = egresos.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (previo != null) {
            if (!previo.getRequestHash().equals(hash)) {
                throw new OperacionNoPermitidaException("La idempotency key ya fue usada con otro contenido");
            }
            return respuesta(previo);
        }
        Usuario usuario = usuarioActivoForUpdate(principal);
        previo = egresos.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (previo != null) {
            if (!previo.getRequestHash().equals(hash)) {
                throw new OperacionNoPermitidaException("La idempotency key ya fue usada con otro contenido");
            }
            return respuesta(previo);
        }
        MetodoPago metodo = metodos.findById(request.metodoPagoId())
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .orElseThrow(() -> new OperacionNoPermitidaException("Método de pago inexistente o inactivo"));
        BigDecimal monto = monedaPositiva(request.monto());
        Egreso egreso = new Egreso();
        egreso.setFecha(request.fecha() == null ? LocalDate.now(clock) : request.fecha());
        egreso.setMonto(monto);
        egreso.setObservaciones(request.observaciones());
        egreso.setMetodoPago(metodo);
        egreso.setEstado(EstadoPago.REGISTRADO);
        egreso.setUsuario(usuario);
        egreso.setIdempotencyKey(request.idempotencyKey());
        egreso.setRequestHash(hash);
        egresos.save(egreso);

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setTipo(TipoMovimientoCaja.EGRESO);
        movimiento.setFecha(egreso.getFecha());
        movimiento.setImporte(monto);
        movimiento.setMetodoPago(metodo);
        movimiento.setEgreso(egreso);
        movimiento.setUsuario(usuario);
        movimiento.setIdempotencyKey("egreso:" + request.idempotencyKey());
        caja.save(movimiento);
        log.info("Egreso registrado id={} monto={}", egreso.getId(), monto.toPlainString());
        return respuesta(egreso);
    }

    @Transactional
    public EgresoResponse anular(Long id, EgresoAnulacionRequest request, Usuario principal) {
        String reversalHash = RequestHash.sha256("ANULAR_EGRESO", id.toString(), request.motivo());
        Egreso egreso = egresos.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("Egreso no encontrado"));
        if (egreso.getEstado() == EstadoPago.ANULADO) {
            if (request.idempotencyKey().equals(egreso.getReversalIdempotencyKey())) {
                if (!reversalHash.equals(egreso.getReversalRequestHash())) {
                    throw new OperacionNoPermitidaException("La idempotency key ya fue usada con otro contenido");
                }
                return respuesta(egreso);
            }
            throw new OperacionNoPermitidaException("El egreso ya fue anulado");
        }
        Usuario usuario = usuarioActivo(principal);
        MovimientoCaja original = caja.findByEgresoIdAndTipo(id, TipoMovimientoCaja.EGRESO)
                .orElseThrow(() -> new IllegalStateException("Egreso sin movimiento de caja"));
        MovimientoCaja reverso = new MovimientoCaja();
        reverso.setTipo(TipoMovimientoCaja.REVERSO);
        reverso.setFecha(LocalDate.now(clock));
        reverso.setImporte(original.getImporte());
        reverso.setMetodoPago(original.getMetodoPago());
        reverso.setEgreso(egreso);
        reverso.setMovimientoRevertido(original);
        reverso.setUsuario(usuario);
        reverso.setIdempotencyKey("anulacion-egreso:" + request.idempotencyKey());
        reverso.setMotivo(request.motivo());
        caja.save(reverso);
        egreso.setEstado(EstadoPago.ANULADO);
        egreso.setMotivoAnulacion(request.motivo());
        egreso.setFechaAnulacion(clock.instant());
        egreso.setReversalIdempotencyKey(request.idempotencyKey());
        egreso.setReversalRequestHash(reversalHash);
        log.info("Egreso anulado id={}", id);
        return respuesta(egreso);
    }

    @Transactional(readOnly = true)
    public EgresoResponse obtenerEgresoPorId(Long id) {
        return respuesta(egresos.findById(id).orElseThrow(() -> new EntityNotFoundException("Egreso no encontrado")));
    }

    @Transactional(readOnly = true)
    public Page<EgresoResponse> listarEgresos(Pageable pageable) {
        return egresos.findAll(pageable).map(this::respuesta);
    }

    private Usuario usuarioActivo(Usuario principal) {
        if (principal == null || principal.getId() == null) {
            throw new OperacionNoPermitidaException("Usuario autenticado requerido");
        }
        return usuarios.findById(principal.getId()).filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new OperacionNoPermitidaException("El usuario está inactivo"));
    }

    private Usuario usuarioActivoForUpdate(Usuario principal) {
        if (principal == null || principal.getId() == null) {
            throw new OperacionNoPermitidaException("Usuario autenticado requerido");
        }
        return usuarios.findByIdForUpdate(principal.getId()).filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new OperacionNoPermitidaException("El usuario está inactivo"));
    }

    private EgresoResponse respuesta(Egreso e) {
        return new EgresoResponse(e.getId(), e.getFecha(), decimal(e.getMonto()), e.getObservaciones(),
                e.getMetodoPago().getId(), e.getUsuario().getId(), e.getEstado().name(), e.getIdempotencyKey());
    }

    private static BigDecimal monedaPositiva(String valor) {
        BigDecimal importe = new BigDecimal(valor).setScale(2, RoundingMode.UNNECESSARY);
        if (importe.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        return importe;
    }

    private static String decimal(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String hash(EgresoRegistroRequest request) {
        String canonico = request.fecha() + "|" + monedaPositiva(request.monto()).toPlainString() + "|"
                + request.metodoPagoId() + "|" + (request.observaciones() == null ? "" : request.observaciones());
        return RequestHash.sha256("REGISTRAR_EGRESO", canonico);
    }
}
