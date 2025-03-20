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
    public MatriculaResponse obtenerOMarcarPendiente(Long alumnoId) {
        int anioActual = Year.now().getValue();
        List<Matricula> matriculas = matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anioActual);
        Matricula matricula;
        if (matriculas.isEmpty()) {
            Alumno alumno = alumnoRepositorio.findById(alumnoId)
                    .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
            matricula = new Matricula();
            matricula.setAlumno(alumno);
            matricula.setAnio(anioActual);
            matricula.setPagada(false);
            matricula = matriculaRepositorio.save(matricula);

            // Registrar el DetallePago correspondiente a la matrícula recién creada.
            registrarDetallePagoMatricula(matricula);
        } else {
            matricula = matriculas.stream()
                    .filter(m -> !m.getPagada())
                    .findFirst()
                    .orElse(matriculas.get(0));
        }
        return matriculaMapper.toResponse(matricula);
    }

    /**
     * Registra un DetallePago para la matrícula usando el concepto "MATRICULA {anio}".
     */
    @Transactional
    public DetallePago registrarDetallePagoMatricula(Matricula matricula) {
        String descripcionConcepto = "MATRICULA " + matricula.getAnio();
        Concepto concepto = conceptoRepositorio.findByDescripcionIgnoreCase(descripcionConcepto)
                .orElseThrow(() -> new IllegalArgumentException("Concepto no encontrado: " + descripcionConcepto));

        DetallePago detalle = new DetallePago();
        detalle.setAlumno(matricula.getAlumno());
        detalle.setDescripcionConcepto(concepto.getDescripcion());
        detalle.setMontoOriginal(concepto.getPrecio());
        detalle.setImporteInicial(concepto.getPrecio());
        detalle.setImportePendiente(concepto.getPrecio());
        detalle.setaCobrar(0.0);
        detalle.setCobrado(false);
        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setFechaRegistro(LocalDate.now());
        detalle.setMatricula(matricula);
        // Aquí asignamos directamente el alumno a partir de la matrícula o inscripción
        detalle.setAlumno(matricula.getAlumno());

        detallePagoRepositorio.save(detalle);
        log.info("DetallePago para Matrícula id={} creado con importeInicial={}",
                matricula.getId(), detalle.getImporteInicial());
        return detalle;
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
}
