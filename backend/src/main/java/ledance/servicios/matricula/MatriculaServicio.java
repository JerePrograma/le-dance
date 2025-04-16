package ledance.servicios.matricula;

import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.*;
import ledance.repositorios.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MatriculaServicio {

    private static final Logger log = LoggerFactory.getLogger(MatriculaServicio.class);

    private final MatriculaRepositorio matriculaRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final MatriculaMapper matriculaMapper;
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio;
    private final PagoRepositorio pagoRepositorio;

    public MatriculaServicio(MatriculaRepositorio matriculaRepositorio, AlumnoRepositorio alumnoRepositorio, MatriculaMapper matriculaMapper, DetallePagoRepositorio detallePagoRepositorio, ConceptoRepositorio conceptoRepositorio, ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio, PagoRepositorio pagoRepositorio) {
        this.matriculaRepositorio = matriculaRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.matriculaMapper = matriculaMapper;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.procesoEjecutadoRepositorio = procesoEjecutadoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
    }

    /**
     * Procesa la matrícula en su propia transacción para que cualquier fallo
     * no marque rollback la TX principal de inscripción.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void obtenerOMarcarPendienteAutomatica(Long alumnoId, Pago pagoPendiente) {
        int anio = LocalDate.now().getYear();
        log.info("[MatriculaAutoService] Procesando matrícula automática para alumno id={}, año={}",
                alumnoId, anio);

        // 1) Obtener o crear matrícula
        Matricula matricula = obtenerOMarcarPendienteMatricula(alumnoId, anio);
        log.info("  ↳ Matrícula pendiente: id={} para alumnoId={}", matricula.getId(), alumnoId);

        // 2) Buscar sólo DETALLES ACTIVOS
        Optional<DetallePago> detalleOpt = detallePagoRepositorio
                .findActiveByMatriculaId(matricula.getId());
        if (detalleOpt.isPresent()) {
            log.info("  ↳ Ya existe DetallePago ACTIVO (id={}) para matrícula id={}",
                    detalleOpt.get().getId(), matricula.getId());
            return;
        }

        // 3) Registrar nuevo detalle si no había ninguno ACTIVO
        registrarDetallePagoMatriculaAutomatica(matricula, pagoPendiente);
    }

    public Matricula obtenerOMarcarPendienteMatricula(Long alumnoId, int anio) {
        return matriculaRepositorio
                .findFirstByAlumnoIdAndAnioOrderByIdDesc(alumnoId, anio)
                .orElseGet(() -> {
                    Alumno alumno = alumnoRepositorio.findById(alumnoId)
                            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
                    Matricula nueva = new Matricula();
                    nueva.setAlumno(alumno);
                    nueva.setAnio(anio);
                    nueva.setPagada(false);
                    nueva = matriculaRepositorio.save(nueva);
                    log.info("    ↳ Nueva matrícula creada: id={} para alumnoId={}", nueva.getId(), alumnoId);
                    return nueva;
                });
    }

    @Transactional
    protected void registrarDetallePagoMatriculaAutomatica(Matricula matricula, Pago pagoPendiente) {
        log.info("[MatriculaAutoService] Creando DetallePago para matrícula id={}", matricula.getId());

        Alumno alumno = matricula.getAlumno();
        Pago pagoAsociado = Optional
                .ofNullable(obtenerUltimoPagoPendienteEntidad(alumno.getId()))
                .orElseGet(() -> {
                    Pago p = new Pago();
                    p.setAlumno(alumno);
                    p.setFecha(LocalDate.now());
                    p.setFechaVencimiento(LocalDate.now().plusDays(30));
                    p.setImporteInicial(0.0);
                    p.setMonto(0.0);
                    p.setSaldoRestante(0.0);
                    p.setEstadoPago(EstadoPago.ACTIVO);
                    pagoRepositorio.save(p);
                    log.info("    ↳ Pago nuevo creado para alumno id={}: pago id={}", alumno.getId(), p.getId());
                    return p;
                });

        DetallePago detalle = new DetallePago();
        detalle.setMatricula(matricula);
        detalle.setAlumno(alumno);
        detalle.setDescripcionConcepto("MATRICULA " + LocalDate.now().getYear());
        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setFechaRegistro(LocalDate.now());

        if (verificarDetallePagoUnico(detalle)) return;

        asignarConceptoDetallePago(detalle);
        double valorBase = detalle.getConcepto().getPrecio();
        detalle.setValorBase(valorBase);
        detalle.setImporteInicial(valorBase);
        detalle.setImportePendiente(valorBase);
        detalle.setACobrar(0.0);
        detalle.setCobrado(false);
        detalle.setPago(pagoAsociado);

        detallePagoRepositorio.save(detalle);
        log.info("    ↳ DetallePago matricula id={} creado: detalle id={}", matricula.getId(), detalle.getId());

        pagoAsociado.getDetallePagos().add(detalle);
        pagoRepositorio.save(pagoAsociado);
        log.info("    ↳ Pago (id={}) actualizado tras matricula: monto={}, saldo={}",
                pagoAsociado.getId(), pagoAsociado.getMonto(), pagoAsociado.getSaldoRestante());
    }

    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        return pagoRepositorio
                .findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(
                        alumnoId, EstadoPago.ACTIVO, 0.0)
                .orElse(null);
    }

    /**
     * Verifica que no exista un DetallePago duplicado para un alumno basado en la descripción y el tipo.
     * Si la descripción contiene "MATRICULA" y ya existe un registro, se lanza una excepción.
     */
    @Transactional
    public boolean verificarDetallePagoUnico(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        if (descripcion != null && descripcion.toUpperCase().contains("MATRICULA")) {
            log.info("[verificarDetallePagoUnico] Verificando duplicidad para alumnoId={} con descripción '{}'", alumnoId, descripcion);
            boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(alumnoId, descripcion, TipoDetallePago.MATRICULA);
            if (existeDetalleDuplicado) {
                log.error("[verificarDetallePagoUnico] DetallePago duplicado encontrado para alumnoId={} con descripción '{}'", alumnoId, descripcion);
                return true;
            }
        } else {
            log.info("[verificarDetallePagoUnico] No se requiere verificación de duplicidad para la descripción '{}'", descripcion);
        }
        return false;
    }

    /**
     * Asigna el Concepto y SubConcepto al DetallePago según la descripción "MATRICULA <anio>".
     */
    private void asignarConceptoDetallePago(DetallePago detalle) {
        int anioActual = Year.now().getValue();
        String descripcionConcepto = "MATRICULA " + anioActual;
        log.info("[asignarConceptoDetallePago] Buscando Concepto con descripción '{}'", descripcionConcepto);

        Optional<Concepto> optConcepto = conceptoRepositorio.findByDescripcionIgnoreCase(descripcionConcepto);
        if (optConcepto.isEmpty()) {
            log.error("[asignarConceptoDetallePago] No se encontró Concepto con descripción '{}' para DetallePago", descripcionConcepto);
            throw new IllegalStateException("No se encontró Concepto para Matrícula con descripción: " + descripcionConcepto);
        }
        Concepto concepto = optConcepto.get();
        detalle.setConcepto(concepto);
        detalle.setSubConcepto(concepto.getSubConcepto());
        log.info("[asignarConceptoDetallePago] Se asignaron Concepto: {} y SubConcepto: {} al DetallePago", detalle.getConcepto(), detalle.getSubConcepto());
    }

    @Transactional
    public void verificarMatriculaNoDuplicada(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("[verificarMatriculaNoDuplicada] Verificando existencia de matrícula para alumnoId={} con descripción '{}'", alumnoId, descripcion);

        // Solo se verifica si la descripción contiene "MATRICULA"
        if (descripcion != null && descripcion.toUpperCase().contains("MATRICULA")) {
            boolean existeDetalleActivo = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPagoNot(alumnoId, descripcion, TipoDetallePago.MATRICULA, EstadoPago.ANULADO);
            if (existeDetalleActivo) {
                log.error("[verificarMatriculaNoDuplicada] Ya existe un DetallePago activo (no anulado) con descripción '{}' para alumnoId={}", descripcion, alumnoId);
                throw new IllegalStateException("MATRICULA YA COBRADA");
            }
        } else {
            log.info("[verificarMatriculaNoDuplicada] La descripción '{}' no contiene 'MATRICULA', no se realiza verificación.", descripcion);
        }
    }

    // Metodo para actualizar el estado de la matricula (ya existente)
    @Transactional
    public MatriculaResponse actualizarEstadoMatricula(Long matriculaId, MatriculaRegistroRequest request) {
        Matricula m = matriculaRepositorio.findById(matriculaId).orElseThrow(() -> new IllegalArgumentException("Matricula no encontrada."));
        matriculaMapper.updateEntityFromRequest(request, m);
        return matriculaMapper.toResponse(matriculaRepositorio.save(m));
    }

    @Transactional
    public void generarMatriculasAnioVigente() {
        LocalDate today = LocalDate.now();
        int anioActual = today.getYear();

        ProcesoEjecutado proceso = procesoEjecutadoRepositorio.findByProceso("MATRICULA_AUTOMATICA")
                .orElse(new ProcesoEjecutado("MATRICULA_AUTOMATICA", null));
        YearMonth mesActual = YearMonth.from(today);
        if (proceso.getUltimaEjecucion() != null && YearMonth.from(proceso.getUltimaEjecucion()).equals(mesActual)) {
            log.info("El proceso MATRICULA_AUTOMATICA ya fue ejecutado este mes: {}", proceso.getUltimaEjecucion());
            return;
        }

        // Obtener alumnos activos con inscripciones
        List<Alumno> alumnosConInscripciones = alumnoRepositorio.findAll().stream()
                .filter(alumno -> Boolean.TRUE.equals(alumno.getActivo()) &&
                        alumno.getInscripciones() != null && !alumno.getInscripciones().isEmpty())
                .collect(Collectors.toList());
        log.info("Total de alumnos con al menos una inscripción activa: {}", alumnosConInscripciones.size());

        for (Alumno alumno : alumnosConInscripciones) {
            log.info("Procesando alumno id: {} - {}", alumno.getId(), alumno.getNombre());
            Optional<Matricula> optMatricula = matriculaRepositorio.findFirstByAlumnoIdAndAnioOrderByIdDesc(alumno.getId(), anioActual);
            Matricula matricula;
            if (optMatricula.isPresent()) {
                matricula = optMatricula.get();
                log.info("Ya existe matrícula para el alumno id={}, matrícula id={}", alumno.getId(), matricula.getId());
            } else {
                matricula = new Matricula();
                matricula.setAlumno(alumno);
                matricula.setAnio(anioActual);
                matricula.setPagada(false);
                matricula = matriculaRepositorio.save(matricula);
                log.info("Nueva matrícula creada para alumno id={}, matrícula id={}", alumno.getId(), matricula.getId());
            }

            // Si no existe DetallePago para la matrícula, se registra.
            if (!detallePagoRepositorio.existsByMatriculaId(matricula.getId())) {
                log.info("No existe DetallePago para la matrícula id={}. Se procede a registrarlo.", matricula.getId());
                registrarDetallePagoMatriculaAutomatica(matricula);
            } else {
                log.info("Ya existe un DetallePago asociado a la matrícula id={}", matricula.getId());
            }
        }
        proceso.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proceso);
        log.info("Proceso MATRICULA_AUTOMATICA completado. Flag actualizado a {}", today);
    }


    @Transactional
    protected void registrarDetallePagoMatriculaAutomatica(Matricula matricula) {
        log.info("[registrarDetallePagoMatricula] Iniciando registro del DetallePago para matrícula id={}", matricula.getId());
        Alumno alumno = matricula.getAlumno();
        // Obtener o crear el mismo pago pendiente que se utiliza para mensualidades
        Pago pagoAsociado = obtenerOPersistirPagoPendiente(alumno.getId());

        DetallePago detalle = new DetallePago();
        detalle.setMatricula(matricula);
        detalle.setAlumno(alumno);

        // Se asigna la descripción según el año actual.
        int anio = LocalDate.now().getYear();
        String descripcionConcepto = "MATRICULA " + anio;
        detalle.setDescripcionConcepto(descripcionConcepto);
        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setFechaRegistro(LocalDate.now());

        // Verificar duplicidad según la lógica de negocio.
        if (verificarDetallePagoUnico(detalle)) {
            log.info("DetallePago duplicado detectado para matrícula id={}. Cancelando creación.", matricula.getId());
            return;
        }
        asignarConceptoDetallePago(detalle);

        Double valorBase = detalle.getConcepto().getPrecio();
        detalle.setValorBase(valorBase);
        detalle.setImporteInicial(valorBase);
        detalle.setImportePendiente(valorBase);
        detalle.setACobrar(0.0);
        detalle.setCobrado(false);
        detalle.setPago(pagoAsociado);

        detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMatricula] DetallePago para matrícula id={} creado y guardado exitosamente.", matricula.getId());

        // Actualizar el pago consolidado acumulando el importe
        if (pagoAsociado.getDetallePagos() == null) {
            pagoAsociado.setDetallePagos(new ArrayList<>());
        }
        pagoAsociado.getDetallePagos().add(detalle);
        pagoAsociado.setMonto((pagoAsociado.getMonto() == null ? 0.0 : pagoAsociado.getMonto()) + valorBase);
        pagoAsociado.setSaldoRestante((pagoAsociado.getSaldoRestante() == null ? 0.0 : pagoAsociado.getSaldoRestante()) + valorBase);
        pagoRepositorio.save(pagoAsociado);
        log.info("[registrarDetallePagoMatricula] Pago (ID={}) actualizado: nuevo monto={} y saldo restante={}",
                pagoAsociado.getId(), pagoAsociado.getMonto(), pagoAsociado.getSaldoRestante());
    }

    @Transactional
    protected Pago obtenerOPersistirPagoPendiente(Long alumnoId) {
        // Se utiliza la búsqueda de un pago "pendiente" según la lógica del repositorio.
        Pago pagoExistente = obtenerUltimoPagoPendienteEntidad(alumnoId);
        if (pagoExistente != null) {
            log.info("Se encontró un pago pendiente para el alumno id={}: Pago id={}", alumnoId, pagoExistente.getId());
            return pagoExistente;
        }
        Pago nuevoPago = new Pago();
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado para id=" + alumnoId));
        nuevoPago.setAlumno(alumno);
        nuevoPago.setFecha(LocalDate.now());
        nuevoPago.setFechaVencimiento(LocalDate.now().plusDays(30));
        nuevoPago.setImporteInicial(0.0);
        nuevoPago.setMonto(0.0);
        nuevoPago.setSaldoRestante(0.0);
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        pagoRepositorio.save(nuevoPago);
        log.info("No se encontró un pago pendiente; se creó un nuevo pago con ID={}", nuevoPago.getId());
        return nuevoPago;
    }

}
