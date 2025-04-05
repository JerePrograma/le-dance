package ledance.servicios.matricula;

import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.*;
import ledance.repositorios.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class MatriculaServicio {

    private static final Logger log = LoggerFactory.getLogger(MatriculaServicio.class);

    private final MatriculaRepositorio matriculaRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final MatriculaMapper matriculaMapper;
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio;

    public MatriculaServicio(MatriculaRepositorio matriculaRepositorio, AlumnoRepositorio alumnoRepositorio, MatriculaMapper matriculaMapper, DetallePagoRepositorio detallePagoRepositorio, ConceptoRepositorio conceptoRepositorio, ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio) {
        this.matriculaRepositorio = matriculaRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.matriculaMapper = matriculaMapper;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.procesoEjecutadoRepositorio = procesoEjecutadoRepositorio;
    }

    @Transactional
    public void obtenerOMarcarPendienteAutomatica(Long alumnoId, Pago pagoPendiente) {
        int anio = LocalDate.now().getYear();
        log.info("[MatriculaAutoService] Procesando matrícula automática para alumno id={}, año={}", alumnoId, anio);

        // Obtiene o crea la matrícula pendiente para el alumno en el año indicado.
        Matricula matricula = obtenerOMarcarPendienteMatricula(alumnoId, anio);
        log.info("Matrícula pendiente: id={} para alumnoId={}", matricula.getId(), alumnoId);

        // Si ya existe un DetallePago asociado a la matrícula, se finaliza el proceso.
        Optional<DetallePago> detalleOpt = detallePagoRepositorio.findByMatriculaId(matricula.getId());
        if (detalleOpt.isPresent()) {
            log.info("DetallePago ya existe para la matrícula id={}", matricula.getId());
            return;
        }

        // Si no existe, se registra un nuevo DetallePago para la matrícula.
        registrarDetallePagoMatriculaAutomatica(matricula, pagoPendiente);
    }

    @Transactional
    public Matricula obtenerOMarcarPendienteMatricula(Long alumnoId, int anio) {
        // Busca la matrícula pendiente ya registrada para el alumno en el año indicado
        return matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anio).orElseGet(() -> {
            Alumno alumno = alumnoRepositorio.findById(alumnoId).orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
            Matricula nuevaMatricula = new Matricula();
            nuevaMatricula.setAlumno(alumno);
            nuevaMatricula.setAnio(anio);
            nuevaMatricula.setPagada(false);
            nuevaMatricula = matriculaRepositorio.save(nuevaMatricula);
            matriculaRepositorio.flush(); // Asegura visibilidad en la transacción actual
            log.info("Nueva matrícula creada: id={} para alumnoId={}", nuevaMatricula.getId(), alumnoId);
            return nuevaMatricula;
        });
    }

    @Transactional
    protected void registrarDetallePagoMatriculaAutomatica(Matricula matricula, Pago pagoPendiente) {
        log.info("[registrarDetallePagoMatricula] Iniciando registro del DetallePago para matrícula id={}", matricula.getId());

        DetallePago detalle = new DetallePago();
        if (pagoPendiente == null) {
            if (detalle.getFechaRegistro() == null) {
                detalle.setFechaRegistro(LocalDate.now());
            }
        } else if (pagoPendiente.getFecha() == null && detalle.getFechaRegistro() == null) {
            detalle.setFechaRegistro(LocalDate.now());
        }
        // Asignar datos obligatorios
        detalle.setMatricula(matricula);
        detalle.setAlumno(matricula.getAlumno());
        int anio = LocalDate.now().getYear();
        String descripcionConcepto = "MATRICULA " + anio;
        detalle.setDescripcionConcepto(descripcionConcepto);
        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setFechaRegistro(LocalDate.now());
        detalle.setVersion(0L);

        // Verificar duplicidad en detalle de pago (se busca por alumno, descripción y tipo)
        verificarDetallePagoUnico(detalle);

        // Asignar Concepto y SubConcepto basados en el año actual
        asignarConceptoDetallePago(detalle);
        Double valorBase = detalle.getConcepto().getPrecio();
        detalle.setValorBase(valorBase);
        detalle.setImporteInicial(valorBase);
        detalle.setImportePendiente(valorBase);
        detalle.setaCobrar(0.0);
        detalle.setCobrado(false);

        detalle.setPago(pagoPendiente);

        detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMatricula] DetallePago para matrícula id={} creado y guardado exitosamente.", matricula.getId());

    }

    /**
     * Verifica que no exista un DetallePago duplicado para un alumno basado en la descripción y el tipo.
     * Si la descripción contiene "MATRICULA" y ya existe un registro, se lanza una excepción.
     */
    @Transactional
    public void verificarDetallePagoUnico(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        if (descripcion != null && descripcion.toUpperCase().contains("MATRICULA")) {
            log.info("[verificarDetallePagoUnico] Verificando duplicidad para alumnoId={} con descripción '{}'", alumnoId, descripcion);
            boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(alumnoId, descripcion, TipoDetallePago.MATRICULA);
            if (existeDetalleDuplicado) {
                log.error("[verificarDetallePagoUnico] DetallePago duplicado encontrado para alumnoId={} con descripción '{}'", alumnoId, descripcion);
                throw new IllegalStateException("MATRICULA YA COBRADA");
            }
        } else {
            log.info("[verificarDetallePagoUnico] No se requiere verificación de duplicidad para la descripción '{}'", descripcion);
        }
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

        ProcesoEjecutado proceso = procesoEjecutadoRepositorio.findByProceso("MATRICULA_AUTOMATICA").orElse(new ProcesoEjecutado("MATRICULA_AUTOMATICA", null));

        YearMonth mesActual = YearMonth.from(today);
        if (proceso.getUltimaEjecucion() != null && YearMonth.from(proceso.getUltimaEjecucion()).equals(mesActual)) {
            log.info("El proceso MATRÍCULA_AUTOMATICA ya fue ejecutado este mes: {}", proceso.getUltimaEjecucion());
            return;
        }

        List<Alumno> alumnosConInscripciones = alumnoRepositorio.findAll().stream().filter(alumno -> Boolean.TRUE.equals(alumno.getActivo()) && alumno.getInscripciones() != null && !alumno.getInscripciones().isEmpty()).toList();

        log.info("Total de alumnos con al menos una inscripción activa: {}", alumnosConInscripciones.size());

        for (Alumno alumno : alumnosConInscripciones) {
            log.info("Procesando alumno id: {} - {}", alumno.getId(), alumno.getNombre());

            Optional<Matricula> optMatricula = matriculaRepositorio.findByAlumnoIdAndAnio(alumno.getId(), anioActual);

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

            // Verificamos si ya hay un DetallePago asociado a la matrícula
            boolean detalleExiste = detallePagoRepositorio.existsByMatriculaId(matricula.getId());

            if (!detalleExiste) {
                log.info("No existe DetallePago para la matrícula id={}. Se procederá a registrar uno.", matricula.getId());
                registrarDetallePagoMatriculaAutomatica(matricula, null); // null porque no hay pago aún
            } else {
                log.info("Ya existe un DetallePago asociado a la matrícula id={}", matricula.getId());
            }
        }

        proceso.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proceso);
        log.info("Proceso MATRÍCULA_AUTOMATICA completado. Flag actualizado a {}", today);
    }

}
