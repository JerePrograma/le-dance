package ledance.servicios.alumno;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.alumno.response.AlumnoDataResponse;
import ledance.dto.deudas.DeudasPendientesResponse;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.alumno.request.AlumnoModificacionRequest;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoDetalleResponse;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Alumno;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.AlumnoRepositorio;
import jakarta.transaction.Transactional;
import ledance.servicios.inscripcion.InscripcionServicio;
import ledance.servicios.pago.PagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlumnoServicio implements IAlumnoServicio {

    private static final Logger log = LoggerFactory.getLogger(AlumnoServicio.class);

    private final AlumnoRepositorio alumnoRepositorio;
    private final AlumnoMapper alumnoMapper;
    private final DisciplinaMapper disciplinaMapper;
    private final InscripcionServicio inscripcionServicio;
    private final PagoServicio pagoServicio;

    public AlumnoServicio(AlumnoRepositorio alumnoRepositorio, AlumnoMapper alumnoMapper, DisciplinaMapper disciplinaMapper, InscripcionServicio inscripcionServicio, PagoServicio pagoServicio) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.alumnoMapper = alumnoMapper;
        this.disciplinaMapper = disciplinaMapper;
        this.inscripcionServicio = inscripcionServicio;
        this.pagoServicio = pagoServicio;
    }

    @Override
    @Transactional
    public AlumnoDetalleResponse registrarAlumno(AlumnoRegistroRequest requestDTO) {
        log.info("Registrando alumno: {}", requestDTO.nombre());

        Alumno alumno = alumnoMapper.toEntity(requestDTO);

        // Calcular edad automaticamente
        if (alumno.getFechaNacimiento() != null) {
            alumno.setEdad(calcularEdad(requestDTO.fechaNacimiento()));
        }

        // Guardar el alumno
        Alumno alumnoGuardado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toDetalleResponse(alumnoGuardado);
    }

    @Override
    public AlumnoDetalleResponse obtenerAlumnoPorId(Long id) {
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        return alumnoMapper.toDetalleResponse(alumno);
    }

    @Override
    public List<AlumnoListadoResponse> listarAlumnos() {
        return alumnoRepositorio.findAll().stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlumnoDetalleResponse actualizarAlumno(Long id, AlumnoModificacionRequest requestDTO) {
        log.info("Actualizando alumno con id: {}", id);
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        alumnoMapper.updateEntityFromRequest(requestDTO, alumno);

        // Recalcular edad si cambia la fecha de nacimiento
        if (alumno.getFechaNacimiento() != null) {
            alumno.setEdad(calcularEdad(requestDTO.fechaNacimiento()));
        }

        Alumno alumnoActualizado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toDetalleResponse(alumnoActualizado);
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
    public List<AlumnoListadoResponse> listarAlumnosSimplificado() {
        return alumnoRepositorio.findByActivoTrue().stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlumnoListadoResponse> buscarPorNombre(String nombre) {
        return alumnoRepositorio.buscarPorNombreCompleto(nombre).stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasDeAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        return alumno.getInscripciones().stream()
                .map(inscripcion -> disciplinaMapper.toListadoResponse(inscripcion.getDisciplina())) // ✅ Uso correcto del mapper
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

    public AlumnoDataResponse obtenerDatosAlumno(Long id) {
        // 1. Obtener el alumno o lanzar excepción si no se encuentra.
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.ResourceNotFoundException("Alumno no encontrado con id: " + id));

        // 2. Convertir la entidad Alumno a un DTO de detalle.
        AlumnoDetalleResponse alumnoDetalle = alumnoMapper.toDetalleResponse(alumno);

        // 3. Consultar inscripciones activas para este alumno.
        List<InscripcionResponse> inscripcionesActivas = inscripcionServicio.listarPorAlumno(id);

        // 4. Consultar deudas pendientes del alumno (por ejemplo, consolidando detalles de pagos)
        DeudasPendientesResponse deudas = pagoServicio.listarDeudasPendientesPorAlumno(id);

        // 5. Consultar el último pago (o el pago pendiente activo) del alumno.
        PagoResponse ultimoPago = pagoServicio.obtenerUltimoPagoPorAlumno(id);

        // 6. Armar el DTO unificado y retornarlo.
        return new AlumnoDataResponse(alumnoDetalle, inscripcionesActivas, deudas, ultimoPago);
    }
}