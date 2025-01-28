package ledance.dto.mappers;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.entidades.Asistencia;
import ledance.entidades.Disciplina;
import ledance.entidades.Alumno;
import org.springframework.stereotype.Component;

@Component
public class AsistenciaMapper {

    public AsistenciaResponseDTO toResponseDTO(Asistencia asistencia) {
        return new AsistenciaResponseDTO(
                asistencia.getId(),
                asistencia.getFecha(),
                asistencia.getPresente(),
                asistencia.getObservacion(),
                asistencia.getAlumno().getId(),
                asistencia.getDisciplina().getId()
        );
    }

    public Asistencia toEntity(AsistenciaRequest requestDTO, Disciplina disciplina, Alumno alumno) {
        Asistencia asistencia = new Asistencia();
        asistencia.setFecha(requestDTO.fecha());
        asistencia.setPresente(requestDTO.presente());
        asistencia.setObservacion(requestDTO.observacion());
        asistencia.setDisciplina(disciplina);
        asistencia.setAlumno(alumno);
        return asistencia;
    }
}
