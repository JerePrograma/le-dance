package ledance.servicios.matricula;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.*;
import ledance.repositorios.*;
import ledance.servicios.mensualidad.MensualidadServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.Optional;

@Service
public class MatriculaServicio {

    private static final Logger log = LoggerFactory.getLogger(MensualidadServicio.class);

    private final MatriculaRepositorio matriculaRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final MatriculaMapper matriculaMapper;
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;

    public MatriculaServicio(MatriculaRepositorio matriculaRepositorio,
                             AlumnoRepositorio alumnoRepositorio,
                             MatriculaMapper matriculaMapper, DetallePagoRepositorio detallePagoRepositorio, ConceptoRepositorio conceptoRepositorio, InscripcionRepositorio inscripcionRepositorio) {
        this.matriculaRepositorio = matriculaRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.matriculaMapper = matriculaMapper;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
    }

    /**
     * Obtiene la matricula pendiente del alumno para el año actual.
     * Si no existe, se crea una nueva pendiente y se registra su DetallePago.
     */
    @Transactional
    public Matricula obtenerOMarcarPendienteMatricula(Long alumnoId, int anio) {
        log.info("[obtenerOMarcarPendiente] Iniciando busqueda de matricula pendiente para alumnoId={}, anio={}", alumnoId, anio);

        Matricula matricula = matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anio)
                .stream()
                .filter(m -> !m.getPagada())
                .findFirst()
                .orElseGet(() -> {
                    log.info("[obtenerOMarcarPendiente] No se encontro matricula no pagada. Creando nueva.");
                    Alumno alumno = alumnoRepositorio.findById(alumnoId)
                            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
                    log.info("[obtenerOMarcarPendiente] Alumno encontrado: id={}, nombre={}", alumno.getId(), alumno.getNombre());

                    Matricula nueva = new Matricula();
                    nueva.setAlumno(alumno);
                    log.info("[obtenerOMarcarPendiente] Asignado alumno a matricula.");

                    nueva.setAnio(anio);
                    log.info("[obtenerOMarcarPendiente] Asignado año a matricula: {}", anio);

                    nueva.setPagada(false);
                    log.info("[obtenerOMarcarPendiente] Marcada matricula como no pagada.");

                    nueva = matriculaRepositorio.save(nueva);
                    log.info("[obtenerOMarcarPendiente] Matricula creada y guardada: id={}", nueva.getId());

                    return nueva;
                });

        log.info("[obtenerOMarcarPendiente] Matricula obtenida: id={}, pagada={}, alumnoId={}",
                matricula.getId(), matricula.getPagada(), alumnoId);

        if (!detallePagoRepositorio.existsByMatriculaId(matricula.getId())) {
            log.info("[obtenerOMarcarPendiente] No existe DetallePago. Se procede a registrar uno nuevo.");
            registrarDetallePagoMatriculaAutomatica(matricula);
            log.info("[obtenerOMarcarPendiente] DetallePago para Matricula id={} creado.", matricula.getId());
        } else {
            log.info("[obtenerOMarcarPendiente] Ya existe un DetallePago para la Matricula id={}.", matricula.getId());
        }

        return matricula;
    }

    @Transactional
    public DetallePago obtenerOMarcarPendienteMatriculaAutomatica(Long alumnoId, int anio, Pago pagoPendiente) {
        log.info("[obtenerOMarcarPendiente] Iniciando busqueda de matricula pendiente para alumnoId={}, anio={}", alumnoId, anio);

        // Se asume que en el repositorio tenemos:
        // Optional<Matricula> findByAlumnoIdAndAnioAndPagadaFalse(Long alumnoId, int anio);
        Optional<Matricula> matriculaOpt = matriculaRepositorio.findByAlumnoIdAndAnioAndPagadaFalse(alumnoId, anio);

        Matricula matricula;
        if (matriculaOpt.isPresent()) {
            matricula = matriculaOpt.get();
            log.info("[obtenerOMarcarPendiente] Matricula pendiente encontrada: id={} para alumnoId={}", matricula.getId(), alumnoId);
        } else {
            log.info("[obtenerOMarcarPendiente] No se encontro matricula pendiente. Creando nueva.");
            Alumno alumno = alumnoRepositorio.findById(alumnoId)
                    .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
            matricula = new Matricula();
            matricula.setAlumno(alumno);
            matricula.setAnio(anio);
            matricula.setPagada(false);
            matricula = matriculaRepositorio.save(matricula);
            matriculaRepositorio.flush(); // Forzar que se haga visible la matricula en la misma transaccion
            log.info("[obtenerOMarcarPendiente] Matricula creada, guardada y flushada: id={}", matricula.getId());
        }

        // Aqui, en lugar de basarnos en la existencia de un DetallePago, reutilizamos la matricula encontrada
        // y luego intentamos obtener el DetallePago asociado (si existe) o crear uno nuevo.
        Optional<DetallePago> detalleOpt = detallePagoRepositorio.findByMatriculaId(matricula.getId());
        if (detalleOpt.isPresent()) {
            log.info("[obtenerOMarcarPendiente] DetallePago existente para la Matricula id={} encontrado.", matricula.getId());
            return detalleOpt.get();
        } else {
            log.info("[obtenerOMarcarPendiente] No existe DetallePago para la matricula. Se procede a registrar uno nuevo.");
            DetallePago detallePago = registrarDetallePagoMatriculaAutomatica(matricula, pagoPendiente);
            log.info("[obtenerOMarcarPendiente] DetallePago para Matricula id={} creado.", matricula.getId());
            return detallePago;
        }
    }

    @Transactional
    protected void registrarDetallePagoMatriculaAutomatica(Matricula matricula) {
        if (detallePagoRepositorio.existsByMatriculaId(matricula.getId())) {
            log.info("[registrarDetallePagoMatricula] Ya existe detalle de pago para matricula id={}", matricula.getId());
            return;
        }
        log.info("[registrarDetallePagoMatricula] Iniciando registro del DetallePago para Matricula id={}", matricula.getId());

        DetallePago detalle = new DetallePago();
        detalle.setVersion(0L);
        detalle.setMatricula(matricula);
        detalle.setAlumno(matricula.getAlumno());
        log.info("[registrarDetallePagoMatricula] Alumno asignado: id={}", matricula.getAlumno().getId());

        // Asignar Concepto y SubConcepto usando un metodo auxiliar, con el año actual concatenado.
        asignarConceptoDetallePago(detalle);

        // Calcular e inicializar los importes.
        Double valorBase = detalle.getConcepto().getPrecio();
        log.info("[registrarDetallePagoMatricula] Valor base obtenido del Concepto: {}", valorBase);
        detalle.setValorBase(valorBase);
        detalle.setImporteInicial(valorBase);
        detalle.setImportePendiente(valorBase);
        detalle.setaCobrar(0.0);
        detalle.setCobrado(false);

        // Asignar tipo y fecha de registro.
        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setFechaRegistro(LocalDate.now());
        log.info("[registrarDetallePagoMatricula] Detalle configurado: Tipo={}, FechaRegistro={}",
                detalle.getTipo(), detalle.getFechaRegistro());

        // Persistir el detalle
        detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMatricula] DetallePago para Matricula id={} creado y guardado exitosamente.", matricula.getId());
    }

    @Transactional
    protected DetallePago registrarDetallePagoMatriculaAutomatica(Matricula matricula, Pago pagoPendiente) {
        log.info("[registrarDetallePagoMatricula] Iniciando registro del DetallePago para Matricula id={}", matricula.getId());

        DetallePago detalle = new DetallePago();
        detalle.setVersion(0L);
        detalle.setMatricula(matricula);
        detalle.setAlumno(matricula.getAlumno());
        log.info("[registrarDetallePagoMatricula] Alumno asignado: id={}", matricula.getAlumno().getId());

        // Asignar Concepto y SubConcepto (por ejemplo, concatenando el año)
        asignarConceptoDetallePago(detalle);
        log.info("[registrarDetallePagoMatricula] Concepto asignado: {}", detalle.getConcepto());

        Double valorBase = detalle.getConcepto().getPrecio();
        log.info("[registrarDetallePagoMatricula] Valor base obtenido del Concepto: {}", valorBase);
        detalle.setValorBase(valorBase);
        detalle.setImporteInicial(valorBase);
        detalle.setImportePendiente(valorBase);
        detalle.setaCobrar(0.0);
        detalle.setCobrado(false);

        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setFechaRegistro(LocalDate.now());
        log.info("[registrarDetallePagoMatricula] Detalle configurado: Tipo={}, FechaRegistro={}",
                detalle.getTipo(), detalle.getFechaRegistro());

        detalle.setPago(pagoPendiente);

        detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMatricula] DetallePago para Matricula id={} creado y guardado exitosamente.", matricula.getId());
        return detalle;
    }

    /**
     * Asigna al DetallePago el Concepto y SubConcepto correspondientes para matricula, usando "MATRICULA <anio>".
     */
    private void asignarConceptoDetallePago(DetallePago detalle) {
        int anioActual = Year.now().getValue();
        String descripcionConcepto = "MATRICULA " + anioActual;
        log.info("[asignarConceptoDetallePago] Buscando Concepto con descripcion: '{}'", descripcionConcepto);

        Optional<Concepto> optConcepto = conceptoRepositorio.findByDescripcionIgnoreCase(descripcionConcepto);
        if (optConcepto.isPresent()) {
            Concepto concepto = optConcepto.get();
            detalle.setConcepto(concepto);
            detalle.setSubConcepto(concepto.getSubConcepto());
            log.info("[asignarConceptoDetallePago] Se asignaron Concepto: {} y SubConcepto: {} al DetallePago",
                    detalle.getConcepto(), detalle.getSubConcepto());
        } else {
            log.error("[asignarConceptoDetallePago] No se encontro Concepto con descripcion '{}' para asignar al DetallePago", descripcionConcepto);
            throw new IllegalStateException("No se encontro Concepto para Matricula con descripcion: " + descripcionConcepto);
        }
    }

    // Metodo para actualizar el estado de la matricula (ya existente)
    @Transactional
    public MatriculaResponse actualizarEstadoMatricula(Long matriculaId, MatriculaRegistroRequest request) {
        Matricula m = matriculaRepositorio.findById(matriculaId)
                .orElseThrow(() -> new IllegalArgumentException("Matricula no encontrada."));
        matriculaMapper.updateEntityFromRequest(request, m);
        return matriculaMapper.toResponse(matriculaRepositorio.save(m));
    }

    public boolean existeMatriculaParaAnio(Long alumnoId, int anio) {
        // Se asume que el repositorio de matriculas tiene un metodo que devuelve una matricula
        // activa (o no pagada) para un alumno en un año determinado.
        // Por ejemplo:
        Optional<Matricula> matriculaOpt = matriculaRepositorio.findByAlumnoIdAndAnioAndPagadaFalse(alumnoId, anio);
        return matriculaOpt.isPresent();
    }

    public Inscripcion obtenerInscripcionActiva(Long alumnoId) {
        // Se asume que el repositorio de inscripciones tiene un metodo para buscar la inscripcion activa
        // para un alumno. Por ejemplo, basandose en un estado "ACTIVA".
        return inscripcionRepositorio.findByAlumnoIdAndEstado(alumnoId, EstadoInscripcion.ACTIVA)
                .orElseThrow(() -> new EntityNotFoundException("No se encontro inscripcion activa para alumno id: " + alumnoId));
    }

    public DetallePago obtenerOMarcarPendienteAutomatica(Long alumnoId, Pago pagoPendiente) {
        log.info("[MatriculaAutoService] Iniciando proceso automatico de matricula para alumno id={}", alumnoId);
        int anio = LocalDate.now().getYear();
        log.info("[MatriculaAutoService] Año actual determinado: {}", anio);
        DetallePago detallePago = obtenerOMarcarPendienteMatriculaAutomatica(alumnoId, anio, pagoPendiente);
        log.info("[MatriculaAutoService] Proceso automatico de matricula completado para alumno id={}", alumnoId);
        return detallePago;
    }

}
