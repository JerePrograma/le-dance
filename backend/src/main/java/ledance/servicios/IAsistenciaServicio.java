package ledance.servicios;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AsistenciaResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface IAsistenciaServicio {
    AsistenciaResponseDTO registrarAsistencia(AsistenciaRequest requestDTO);
    List<AsistenciaResponseDTO> obtenerAsistenciasPorDisciplina(Long disciplinaId);
    List<AsistenciaResponseDTO> obtenerAsistenciasPorAlumno(Long alumnoId);
    List<AsistenciaResponseDTO> obtenerAsistenciasPorFechaYDisciplina(LocalDate fecha, Long disciplinaId);
    List<String> generarReporteAsistencias();
}
