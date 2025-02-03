package ledance.servicios;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.dto.response.DisciplinaSimpleResponse;
import ledance.dto.response.ProfesorResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Disciplina;
import ledance.entidades.Asistencia;
import ledance.dto.mappers.AsistenciaMapper;
import ledance.entidades.Profesor;
import ledance.repositorios.AsistenciaRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.AlumnoRepositorio;
import jakarta.transaction.Transactional;
import ledance.repositorios.ProfesorRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AsistenciaServicio implements IAsistenciaServicio {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaServicio.class);

    private final AsistenciaRepositorio asistenciaRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final AsistenciaMapper asistenciaMapper;
    private final ProfesorRepositorio profesorRepositorio;

    public AsistenciaServicio(AsistenciaRepositorio asistenciaRepositorio,
                              DisciplinaRepositorio disciplinaRepositorio,
                              AlumnoRepositorio alumnoRepositorio,
                              AsistenciaMapper asistenciaMapper, ProfesorRepositorio profesorRepositorio) {
        this.asistenciaRepositorio = asistenciaRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.asistenciaMapper = asistenciaMapper;
        this.profesorRepositorio = profesorRepositorio;
    }

    @Override
    @Transactional
    public AsistenciaResponseDTO registrarAsistencia(AsistenciaRequest requestDTO) {
        log.info("Registrando asistencia para alumnoId: {} en disciplinaId: {}", requestDTO.alumnoId(), requestDTO.disciplinaId());

        Disciplina disciplina = disciplinaRepositorio.findById(requestDTO.disciplinaId())
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));

        Alumno alumno = alumnoRepositorio.findById(requestDTO.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        Profesor profesor = requestDTO.profesorId() != null ?
                profesorRepositorio.findById(requestDTO.profesorId()).orElse(null) : null;

        Asistencia asistencia = asistenciaMapper.toEntity(requestDTO);
        asistencia.setDisciplina(disciplina);
        asistencia.setAlumno(alumno);
        asistencia.setProfesor(profesor); // ✅ Asignar el profesor si esta presente

        Asistencia guardada = asistenciaRepositorio.save(asistencia);
        return asistenciaMapper.toResponseDTO(guardada);
    }

    @Override
    public List<AsistenciaResponseDTO> obtenerAsistenciasPorDisciplina(Long disciplinaId) {
        return asistenciaRepositorio.findByDisciplinaId(disciplinaId).stream()
                .map(asistenciaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AsistenciaResponseDTO> obtenerAsistenciasPorAlumno(Long alumnoId) {
        return asistenciaRepositorio.findByAlumnoId(alumnoId).stream()
                .map(asistenciaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AsistenciaResponseDTO> obtenerAsistenciasPorFechaYDisciplina(LocalDate fecha, Long disciplinaId) {
        return asistenciaRepositorio.findByFechaAndDisciplinaId(fecha, disciplinaId).stream()
                .map(asistenciaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> generarReporteAsistencias() {
        List<Object[]> resultados = asistenciaRepositorio.obtenerAsistenciasPorAlumnoYDisciplina();
        return resultados.stream()
                .map(r -> "Alumno: " + r[0] + " | Disciplina: " + r[1] + " | Asistencias: " + r[2])
                .collect(Collectors.toList());
    }

    @Override
    public List<AsistenciaResponseDTO> listarTodasAsistencias() {
        return asistenciaRepositorio.findAll().stream()
                .map(asistencia -> new AsistenciaResponseDTO(
                        asistencia.getId(),
                        asistencia.getFecha(),
                        asistencia.getPresente(),
                        asistencia.getObservacion(),
                        new AlumnoListadoResponse(
                                asistencia.getAlumno().getId(),
                                asistencia.getAlumno().getNombre(),
                                asistencia.getAlumno().getApellido(),
                                asistencia.getAlumno().getActivo() // ✅ Agregado para frontend
                        ),
                        new DisciplinaSimpleResponse(
                                asistencia.getDisciplina().getId(),
                                asistencia.getDisciplina().getNombre()
                        ),
                        asistencia.getProfesor() != null ?
                                new ProfesorResponse(
                                        asistencia.getProfesor().getId(),
                                        asistencia.getProfesor().getNombre(),
                                        asistencia.getProfesor().getApellido(),
                                        asistencia.getProfesor().getEspecialidad(),
                                        asistencia.getProfesor().getActivo(),
                                        Collections.emptyList() // ✅ Evita problemas de dependencias cíclicas
                                ) : null
                )).collect(Collectors.toList());
    }

}
