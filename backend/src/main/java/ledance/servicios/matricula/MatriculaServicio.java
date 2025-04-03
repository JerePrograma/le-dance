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
import java.time.YearMonth;
import java.util.List;
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
    private final ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio;

    public MatriculaServicio(MatriculaRepositorio matriculaRepositorio,
                             AlumnoRepositorio alumnoRepositorio,
                             MatriculaMapper matriculaMapper, DetallePagoRepositorio detallePagoRepositorio, ConceptoRepositorio conceptoRepositorio, InscripcionRepositorio inscripcionRepositorio, ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio) {
        this.matriculaRepositorio = matriculaRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.matriculaMapper = matriculaMapper;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.procesoEjecutadoRepositorio = procesoEjecutadoRepositorio;
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

            // Crear el detalle pero primero armarlo para validarlo
            DetallePago detalleTemporal = new DetallePago();
            detalleTemporal.setAlumno(matricula.getAlumno());
            detalleTemporal.setMatricula(matricula);
            detalleTemporal.setDescripcionConcepto("MATRICULA " + anio); // importante para que se valide correctamente
            detalleTemporal.setTipo(TipoDetallePago.MATRICULA);

            // 👇 Verificamos que no haya una matrícula duplicada
            verificarMatriculaNoDuplicada(detalleTemporal);

            // Si pasó la verificación, creamos el registro final
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

        // --- 🔒 Verificación para evitar duplicados ---
        verificarMatriculaNoDuplicada(detalle); // 👈 se invoca aquí antes de guardar

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

    public DetallePago obtenerOMarcarPendienteAutomatica(Long alumnoId, Pago pagoPendiente) {
        log.info("[MatriculaAutoService] Iniciando proceso automatico de matricula para alumno id={}", alumnoId);
        int anio = LocalDate.now().getYear();
        log.info("[MatriculaAutoService] Año actual determinado: {}", anio);
        DetallePago detallePago = obtenerOMarcarPendienteMatriculaAutomatica(alumnoId, anio, pagoPendiente);
        log.info("[MatriculaAutoService] Proceso automatico de matricula completado para alumno id={}", alumnoId);
        return detallePago;
    }

    @Transactional
    public void generarMatriculasAnioVigente() {
        LocalDate today = LocalDate.now();
        int anioActual = today.getYear();

        ProcesoEjecutado proceso = procesoEjecutadoRepositorio
                .findByProceso("MATRICULA_AUTOMATICA")
                .orElse(new ProcesoEjecutado("MATRICULA_AUTOMATICA", null));

        YearMonth mesActual = YearMonth.from(today);
        if (proceso.getUltimaEjecucion() != null && YearMonth.from(proceso.getUltimaEjecucion()).equals(mesActual)) {
            log.info("El proceso MATRÍCULA_AUTOMATICA ya fue ejecutado este mes: {}", proceso.getUltimaEjecucion());
            return;
        }

        List<Alumno> alumnosConInscripciones = alumnoRepositorio
                .findAll()
                .stream()
                .filter(alumno -> Boolean.TRUE.equals(alumno.getActivo()) && alumno.getInscripciones() != null && !alumno.getInscripciones().isEmpty())
                .toList();

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

    @Transactional
    public void verificarMatriculaNoDuplicada(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("[verificarMatriculaNoDuplicada] Verificando existencia de matrícula o detalle de pago para alumnoId={} con descripción '{}'",
                alumnoId, descripcion);

        // Solo se verifica si la descripción contiene "MATRICULA"
        if (descripcion != null && descripcion.toUpperCase().contains("MATRICULA")) {
            // Ejemplo: Se busca la matrícula para el alumno para el año actual.
            int anioActual = LocalDate.now().getYear();
            log.info("[verificarMatriculaNoDuplicada] Buscando matrícula para alumnoId={} en el año {}", alumnoId, anioActual);
            Optional<Matricula> matriculaOpt = matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anioActual);

            if (matriculaOpt.isPresent()) {
                Matricula matricula = matriculaOpt.get();
                log.info("[verificarMatriculaNoDuplicada] Matrícula encontrada: id={}, pagada={}", matricula.getId(), matricula.getPagada());
                // Si la matrícula ya está pagada, se considera duplicada.
                log.info("[verificarMatriculaNoDuplicada] Matrícula ya pagada para alumnoId={}", alumnoId);
                // Verificar si existe un detalle de pago de tipo MATRÍCULA duplicado.
                boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(
                        alumnoId, descripcion, TipoDetallePago.MATRICULA, EstadoPago.HISTORICO);
                boolean existeDetalleDuplicado2 = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(
                        alumnoId, descripcion, TipoDetallePago.MATRICULA, EstadoPago.ACTIVO);
                log.info("[verificarMatriculaNoDuplicada] Resultado verificación detalle duplicado: {}", existeDetalleDuplicado);
                if (existeDetalleDuplicado || existeDetalleDuplicado2) {
                    log.error("[verificarMatriculaNoDuplicada] Ya existe una matrícula o detalle de pago con descripción '{}' para alumnoId={}",
                            descripcion, alumnoId);
                    throw new IllegalStateException("MATRICULA YA COBRADA");
                }
            } else {
                log.info("[verificarMatriculaNoDuplicada] No se encontró matrícula para alumnoId={} en el año {}", alumnoId, anioActual);
            }
        } else {
            log.info("[verificarMatriculaNoDuplicada] La descripción '{}' no contiene 'MATRICULA', no se realiza verificación.", descripcion);
        }
    }

}
