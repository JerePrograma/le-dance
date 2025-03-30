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
        log.info("[obtenerOMarcarPendienteMatricula] Iniciando búsqueda de matrícula pendiente para alumnoId={}, anio={}", alumnoId, anio);

        // Buscar matrícula pendiente ya registrada (no pagada)
        Optional<Matricula> matriculaOpt = matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anio);
        if (matriculaOpt.isPresent()) {
            Matricula matriculaExistente = matriculaOpt.get();
            log.info("[obtenerOMarcarPendienteMatricula] Matrícula pendiente ya existe para alumnoId={} y anio={} con id={}",
                    alumnoId, anio, matriculaExistente.getId());
            return matriculaExistente;
        } else {
            log.info("[obtenerOMarcarPendienteMatricula] No se encontró matrícula pendiente para alumnoId={} y anio={}. Se procederá a crear una nueva.", alumnoId, anio);
            Alumno alumno = alumnoRepositorio.findById(alumnoId)
                    .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
            log.info("[obtenerOMarcarPendienteMatricula] Alumno encontrado: id={}, nombre={}", alumno.getId(), alumno.getNombre());

            Matricula nuevaMatricula = new Matricula();
            nuevaMatricula.setAlumno(alumno);
            nuevaMatricula.setAnio(anio);
            nuevaMatricula.setPagada(false);
            nuevaMatricula = matriculaRepositorio.save(nuevaMatricula);
            matriculaRepositorio.flush(); // Forzar que la nueva matrícula se haga visible en la transacción
            log.info("[obtenerOMarcarPendienteMatricula] Nueva matrícula creada y guardada: id={}", nuevaMatricula.getId());
            return nuevaMatricula;
        }
    }

    @Transactional
    public DetallePago obtenerOMarcarPendienteMatriculaAutomatica(Long alumnoId, int anio, Pago pagoPendiente) {
        log.info("[obtenerOMarcarPendienteMatriculaAutomatica] Iniciando proceso para alumnoId={}, anio={}", alumnoId, anio);

        // Obtener o crear la matrícula pendiente sin duplicar
        Matricula matricula = obtenerOMarcarPendienteMatricula(alumnoId, anio);
        log.info("[obtenerOMarcarPendienteMatriculaAutomatica] Matrícula obtenida: id={}, alumnoId={}", matricula.getId(), alumnoId);

        // Buscar si ya existe un DetallePago asociado a esta matrícula
        Optional<DetallePago> detalleOpt = detallePagoRepositorio.findByMatriculaId(matricula.getId());
        if (detalleOpt.isPresent()) {
            log.info("[obtenerOMarcarPendienteMatriculaAutomatica] DetallePago existente encontrado para la matrícula id={}", matricula.getId());
            return detalleOpt.get();
        } else {
            log.info("[obtenerOMarcarPendienteMatriculaAutomatica] No existe DetallePago para la matrícula id={}. Se procederá a registrar uno nuevo.", matricula.getId());
            DetallePago detallePago = registrarDetallePagoMatriculaAutomatica(matricula, pagoPendiente);
            log.info("[obtenerOMarcarPendienteMatriculaAutomatica] DetallePago creado para la matrícula id={}", matricula.getId());
            return detallePago;
        }
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
        Optional<Matricula> matriculaOpt = matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anio);
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

    public void generarMatriculasAnioVigente() {
    }
}
