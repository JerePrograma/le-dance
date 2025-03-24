package ledance.servicios.alumno;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoDataResponse;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.*;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.AlumnoRepositorio;
import jakarta.transaction.Transactional;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.MensualidadRepositorio;
import ledance.servicios.inscripcion.InscripcionServicio;
import ledance.servicios.pago.PagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlumnoServicio implements IAlumnoServicio {

    private static final Logger log = LoggerFactory.getLogger(AlumnoServicio.class);

    private final AlumnoRepositorio alumnoRepositorio;
    private final AlumnoMapper alumnoMapper;
    private final DisciplinaMapper disciplinaMapper;
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final DetallePagoMapper detallePagoMapper;

    public AlumnoServicio(AlumnoRepositorio alumnoRepositorio, AlumnoMapper alumnoMapper, DisciplinaMapper disciplinaMapper,
                          DetallePagoRepositorio detallePagoRepositorio, DetallePagoMapper detallePagoMapper, MensualidadRepositorio mensualidadRepositorio) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.alumnoMapper = alumnoMapper;
        this.disciplinaMapper = disciplinaMapper;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoMapper = detallePagoMapper;
    }

    @Override
    @Transactional
    public AlumnoResponse registrarAlumno(AlumnoRegistroRequest requestDTO) {
        log.info("Registrando alumno: {}", requestDTO.nombre());

        Alumno alumno = alumnoMapper.toEntity(requestDTO);

        // Calcular edad automaticamente
        if (alumno.getFechaNacimiento() != null) {
            alumno.setEdad(calcularEdad(requestDTO.fechaNacimiento()));
        }

        // Guardar el alumno
        Alumno alumnoGuardado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toResponse(alumnoGuardado);
    }

    @Override
    public AlumnoResponse obtenerAlumnoPorId(Long id) {
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        return alumnoMapper.toResponse(alumno);
    }

    @Override
    public List<AlumnoResponse> listarAlumnos() {
        return alumnoRepositorio.findAll().stream()
                .map(alumnoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlumnoResponse actualizarAlumno(Long id, AlumnoRegistroRequest requestDTO) {
        log.info("Actualizando alumno con id: {}", id);
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        alumnoMapper.updateEntityFromRequest(requestDTO, alumno);

        // Recalcular edad si cambia la fecha de nacimiento
        if (alumno.getFechaNacimiento() != null) {
            alumno.setEdad(calcularEdad(requestDTO.fechaNacimiento()));
        }

        Alumno alumnoActualizado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toResponse(alumnoActualizado);
    }

    @Override
    @Transactional
    public void darBajaAlumno(Long id) {
        log.info("Dando de baja (baja logica) al alumno con id: {}", id);
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        alumno.setActivo(false);
        alumno.setFechaDeBaja(LocalDate.now());  // Guardar la fecha de baja
        alumnoRepositorio.save(alumno);
    }

    @Override
    public List<AlumnoResponse> listarAlumnosSimplificado() {
        return alumnoRepositorio.findByActivoTrue().stream()
                .map(alumnoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlumnoResponse> buscarPorNombre(String nombre) {
        return alumnoRepositorio.buscarPorNombreCompleto(nombre).stream()
                .map(alumnoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DisciplinaResponse> obtenerDisciplinasDeAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        return alumno.getInscripciones().stream()
                .map(inscripcion -> disciplinaMapper.toResponse(inscripcion.getDisciplina())) // ✅ Uso correcto del mapper
                .collect(Collectors.toList());
    }

    private int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento != null) {
            return Period.between(fechaNacimiento, LocalDate.now()).getYears();
        }
        return 0;
    }

    @Transactional
    public void eliminarAlumno(Long id) {
        log.info("Eliminando al alumno con id: {}", id);
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        alumnoRepositorio.delete(alumno);
    }

    @Transactional
    public AlumnoDataResponse obtenerAlumnoData(Long alumnoId) {
        // 1. Obtener el alumno (con su informacion basica)
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new EntityNotFoundException("Alumno no encontrado"));

        // 2. Obtener todos los DetallePago con importe pendiente mayor a 0
        List<DetallePago> detallesPendientes = detallePagoRepositorio
                .findByAlumnoIdAndImportePendienteGreaterThan(alumnoId, 0.0);

        // 3. Mapear cada DetallePago a su DTO, utilizando el mapper centralizado
        List<DetallePagoResponse> detallePagosDTO = detallesPendientes.stream()
                .map(detallePagoMapper::toDTO)
                .collect(Collectors.toList());

        // 4. Mapear el alumno a AlumnoListadoResponse usando el nuevo mapper
        AlumnoListadoResponse alumnoListadoResponse = alumnoMapper.toAlumnoListadoResponse(alumno);

        // 5. Retornar la respuesta unificada
        return new AlumnoDataResponse(alumnoListadoResponse, detallePagosDTO);
    }

}