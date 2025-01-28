package ledance.servicios;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.entidades.Asistencia;
import ledance.entidades.Disciplina;
import ledance.entidades.Alumno;
import ledance.dto.mappers.AsistenciaMapper;
import ledance.repositorios.AsistenciaRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.AlumnoRepositorio;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AsistenciaServicio {

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

    public AsistenciaResponseDTO registrarAsistencia(AsistenciaRequest requestDTO) {
        Disciplina disciplina = disciplinaRepositorio.findById(requestDTO.disciplinaId())
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        Alumno alumno = alumnoRepositorio.findById(requestDTO.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        Asistencia asistencia = asistenciaMapper.toEntity(requestDTO, disciplina, alumno);
        return asistenciaMapper.toResponseDTO(asistenciaRepositorio.save(asistencia));
    }

    public List<AsistenciaResponseDTO> obtenerAsistenciasPorDisciplina(Long disciplinaId) {
        return asistenciaRepositorio.findByDisciplinaId(disciplinaId).stream()
                .map(asistenciaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<AsistenciaResponseDTO> obtenerAsistenciasPorAlumno(Long alumnoId) {
        return asistenciaRepositorio.findByAlumnoId(alumnoId).stream()
                .map(asistenciaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<AsistenciaResponseDTO> obtenerAsistenciasPorFechaYDisciplina(LocalDate fecha, Long disciplinaId) {
        return asistenciaRepositorio.findByFechaAndDisciplinaId(fecha, disciplinaId).stream()
                .map(asistenciaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}
