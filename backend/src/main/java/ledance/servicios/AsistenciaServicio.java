package ledance.servicios;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.entidades.Alumno;
import ledance.entidades.Disciplina;
import ledance.entidades.Asistencia;
import ledance.dto.mappers.AsistenciaMapper;
import ledance.repositorios.AsistenciaRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.AlumnoRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AsistenciaServicio implements IAsistenciaServicio {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaServicio.class);

    private final AsistenciaRepositorio asistenciaRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final AsistenciaMapper asistenciaMapper;

    public AsistenciaServicio(AsistenciaRepositorio asistenciaRepositorio,
                              DisciplinaRepositorio disciplinaRepositorio,
                              AlumnoRepositorio alumnoRepositorio,
                              AsistenciaMapper asistenciaMapper) {
        this.asistenciaRepositorio = asistenciaRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.asistenciaMapper = asistenciaMapper;
    }

    @Override
    @Transactional
    public AsistenciaResponseDTO registrarAsistencia(AsistenciaRequest requestDTO) {
        log.info("Registrando asistencia para alumnoId: {} en disciplinaId: {}", requestDTO.alumnoId(), requestDTO.disciplinaId());
        Disciplina disciplina = disciplinaRepositorio.findById(requestDTO.disciplinaId())
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        Alumno alumno = alumnoRepositorio.findById(requestDTO.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        Asistencia asistencia = asistenciaMapper.toEntity(requestDTO);
        // Asignar las asociaciones obtenidas
        asistencia.setDisciplina(disciplina);
        asistencia.setAlumno(alumno);
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
}
