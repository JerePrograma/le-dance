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
     * Obtiene la matrícula pendiente del alumno para el año actual.
     * Si no existe, se crea una nueva pendiente y se registra su DetallePago.
     */
    @Transactional
    public Matricula obtenerOMarcarPendienteMatricula(Long alumnoId, int anio) {
        log.info("[obtenerOMarcarPendiente] Iniciando búsqueda de matrícula pendiente para alumnoId={}, anio={}", alumnoId, anio);

        Matricula matricula = matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anio)
                .stream()
                .filter(m -> !m.getPagada())
                .findFirst()
                .orElseGet(() -> {
                    log.info("[obtenerOMarcarPendiente] No se encontró matrícula no pagada. Creando nueva.");
                    Alumno alumno = alumnoRepositorio.findById(alumnoId)
                            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
                    log.info("[obtenerOMarcarPendiente] Alumno encontrado: id={}, nombre={}", alumno.getId(), alumno.getNombre());

                    Matricula nueva = new Matricula();
                    nueva.setAlumno(alumno);
                    log.info("[obtenerOMarcarPendiente] Asignado alumno a matrícula.");

                    nueva.setAnio(anio);
                    log.info("[obtenerOMarcarPendiente] Asignado año a matrícula: {}", anio);

                    nueva.setPagada(false);
                    log.info("[obtenerOMarcarPendiente] Marcada matrícula como no pagada.");

                    nueva = matriculaRepositorio.save(nueva);
                    log.info("[obtenerOMarcarPendiente] Matrícula creada y guardada: id={}", nueva.getId());

                    return nueva;
                });

        log.info("[obtenerOMarcarPendiente] Matrícula obtenida: id={}, pagada={}, alumnoId={}",
                matricula.getId(), matricula.getPagada(), alumnoId);

        if (!detallePagoRepositorio.existsByMatriculaId(matricula.getId())) {
            log.info("[obtenerOMarcarPendiente] No existe DetallePago. Se procede a registrar uno nuevo.");
            registrarDetallePagoMatricula(matricula);
            log.info("[obtenerOMarcarPendiente] DetallePago para Matrícula id={} creado.", matricula.getId());
        } else {
            log.info("[obtenerOMarcarPendiente] Ya existe un DetallePago para la Matrícula id={}.", matricula.getId());
        }

        return matricula;
    }

    @Transactional
    protected void registrarDetallePagoMatricula(Matricula matricula) {
        log.info("[registrarDetallePagoMatricula] Iniciando registro del DetallePago para Matrícula id={}", matricula.getId());

        DetallePago detalle = new DetallePago();
        detalle.setVersion(0L);
        detalle.setMatricula(matricula);
        detalle.setAlumno(matricula.getAlumno());
        log.info("[registrarDetallePagoMatricula] Alumno asignado: id={}", matricula.getAlumno().getId());

        // Asignar Concepto y SubConcepto usando un método auxiliar, con el año actual concatenado.
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
        log.info("[registrarDetallePagoMatricula] DetallePago para Matrícula id={} creado y guardado exitosamente.", matricula.getId());
    }

    /**
     * Asigna al DetallePago el Concepto y SubConcepto correspondientes para matrícula, usando "MATRICULA <anio>".
     */
    private void asignarConceptoDetallePago(DetallePago detalle) {
        int anioActual = Year.now().getValue();
        String descripcionConcepto = "MATRICULA " + anioActual;
        log.info("[asignarConceptoDetallePago] Buscando Concepto con descripción: '{}'", descripcionConcepto);

        Optional<Concepto> optConcepto = conceptoRepositorio.findByDescripcionIgnoreCase(descripcionConcepto);
        if (optConcepto.isPresent()) {
            Concepto concepto = optConcepto.get();
            detalle.setConcepto(concepto);
            detalle.setSubConcepto(concepto.getSubConcepto());
            log.info("[asignarConceptoDetallePago] Se asignaron Concepto: {} y SubConcepto: {} al DetallePago",
                    detalle.getConcepto(), detalle.getSubConcepto());
        } else {
            log.error("[asignarConceptoDetallePago] No se encontró Concepto con descripción '{}' para asignar al DetallePago", descripcionConcepto);
            throw new IllegalStateException("No se encontró Concepto para Matrícula con descripción: " + descripcionConcepto);
        }
    }

    // Método para actualizar el estado de la matrícula (ya existente)
    @Transactional
    public MatriculaResponse actualizarEstadoMatricula(Long matriculaId, MatriculaRegistroRequest request) {
        Matricula m = matriculaRepositorio.findById(matriculaId)
                .orElseThrow(() -> new IllegalArgumentException("Matrícula no encontrada."));
        matriculaMapper.updateEntityFromRequest(request, m);
        return matriculaMapper.toResponse(matriculaRepositorio.save(m));
    }

    public boolean existeMatriculaParaAnio(Long alumnoId, int anio) {
        // Se asume que el repositorio de matrículas tiene un método que devuelve una matrícula
        // activa (o no pagada) para un alumno en un año determinado.
        // Por ejemplo:
        Optional<Matricula> matriculaOpt = matriculaRepositorio.findByAlumnoIdAndAnioAndPagadaFalse(alumnoId, anio);
        return matriculaOpt.isPresent();
    }

    public Inscripcion obtenerInscripcionActiva(Long alumnoId) {
        // Se asume que el repositorio de inscripciones tiene un método para buscar la inscripción activa
        // para un alumno. Por ejemplo, basándose en un estado "ACTIVA".
        return inscripcionRepositorio.findByAlumnoIdAndEstado(alumnoId, EstadoInscripcion.ACTIVA)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró inscripción activa para alumno id: " + alumnoId));
    }

    public void obtenerOMarcarPendienteAutomatica(Long alumnoId) {
        log.info("[MatriculaAutoService] Iniciando proceso automático de matrícula para alumno id={}", alumnoId);
        int anio = LocalDate.now().getYear();
        log.info("[MatriculaAutoService] Año actual determinado: {}", anio);
        obtenerOMarcarPendienteMatricula(alumnoId, anio);
        log.info("[MatriculaAutoService] Proceso automático de matrícula completado para alumno id={}", alumnoId);
    }

}
