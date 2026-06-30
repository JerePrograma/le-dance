package ledance.servicios.alumno;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoDataResponse;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.entidades.Alumno;
import ledance.entidades.EstadoInscripcion;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.servicios.cargo.CargoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class AlumnoServicio {
    private static final Logger log = LoggerFactory.getLogger(AlumnoServicio.class);

    private final AlumnoRepositorio alumnos;
    private final InscripcionRepositorio inscripciones;
    private final AlumnoMapper mapper;
    private final DisciplinaMapper disciplinaMapper;
    private final CargoServicio cargos;
    private final Clock clock;

    public AlumnoServicio(AlumnoRepositorio alumnos,
                          InscripcionRepositorio inscripciones,
                          AlumnoMapper mapper,
                          DisciplinaMapper disciplinaMapper,
                          CargoServicio cargos,
                          Clock clock) {
        this.alumnos = alumnos;
        this.inscripciones = inscripciones;
        this.mapper = mapper;
        this.disciplinaMapper = disciplinaMapper;
        this.cargos = cargos;
        this.clock = clock;
    }

    @Transactional
    public AlumnoResponse registrarAlumno(AlumnoRegistroRequest request) {
        if (alumnos.existsByNombreIgnoreCaseAndApellidoIgnoreCase(request.nombre().trim(), request.apellido().trim())) {
            throw new IllegalStateException("Ya existe un alumno con ese nombre y apellido");
        }
        Alumno alumno = mapper.toEntity(request);
        alumno.setId(null);
        alumno.setFechaIncorporacion(request.fechaIncorporacion() == null
                ? LocalDate.now(clock) : request.fechaIncorporacion());
        alumno = alumnos.save(alumno);
        log.info("Alumno registrado id={}", alumno.getId());
        return respuesta(alumno);
    }

    @Transactional(readOnly = true)
    public AlumnoResponse obtenerAlumnoPorId(Long id) {
        return respuesta(activo(id));
    }

    @Transactional(readOnly = true)
    public List<AlumnoResponse> listarAlumnos() {
        return alumnos.findAll().stream().map(this::respuesta).toList();
    }

    @Transactional
    public AlumnoResponse actualizarAlumno(Long id, AlumnoRegistroRequest request) {
        Alumno alumno = activo(id);
        mapper.updateEntityFromRequest(request, alumno);
        return respuesta(alumno);
    }

    @Transactional
    public void darBajaAlumno(Long id) {
        Alumno alumno = alumnos.findById(id).orElseThrow(() -> new EntityNotFoundException("Alumno no encontrado"));
        if (Boolean.TRUE.equals(alumno.getActivo())) {
            alumno.setActivo(false);
            alumno.setFechaDeBaja(LocalDate.now(clock));
            log.info("Alumno dado de baja id={}", id);
        }
    }

    @Transactional(readOnly = true)
    public List<AlumnoResponse> listarAlumnosSimplificado() {
        return alumnos.findByActivoTrue().stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public List<AlumnoResponse> buscarPorNombre(String nombre) {
        return alumnos.buscarPorNombreCompleto(nombre).stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public List<DisciplinaResponse> obtenerDisciplinasDeAlumno(Long alumnoId) {
        activo(alumnoId);
        return inscripciones.findAllByAlumno_IdAndEstado(alumnoId, EstadoInscripcion.ACTIVA).stream()
                .map(i -> disciplinaMapper.toResponse(i.getDisciplina())).toList();
    }

    @Transactional(readOnly = true)
    public AlumnoDataResponse obtenerAlumnoData(Long alumnoId) {
        Alumno alumno = activo(alumnoId);
        AlumnoListadoResponse base = mapper.toAlumnoListadoResponse(alumno);
        AlumnoListadoResponse conEdad = new AlumnoListadoResponse(base.id(), base.nombre(), base.apellido(),
                base.fechaNacimiento(), base.fechaIncorporacion(), edad(base.fechaNacimiento()), base.celular1(),
                base.celular2(), base.email(), base.documento(), base.fechaDeBaja(), base.nombrePadres(),
                base.autorizadoParaSalirSolo(), base.activo(), base.otrasNotas());
        return new AlumnoDataResponse(conEdad, cargos.listarPendientes(alumnoId));
    }

    private Alumno activo(Long id) {
        return alumnos.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Alumno no encontrado"));
    }

    private AlumnoResponse respuesta(Alumno alumno) {
        AlumnoResponse base = mapper.toResponse(alumno);
        return new AlumnoResponse(base.id(), base.nombre(), base.apellido(), base.fechaNacimiento(),
                base.fechaIncorporacion(), edad(base.fechaNacimiento()), base.celular1(), base.celular2(),
                base.email(), base.documento(), base.fechaDeBaja(), base.nombrePadres(),
                base.autorizadoParaSalirSolo(), base.activo(), base.otrasNotas(), List.of());
    }

    private int edad(LocalDate nacimiento) {
        return nacimiento == null ? 0 : Period.between(nacimiento, LocalDate.now(clock)).getYears();
    }
}
