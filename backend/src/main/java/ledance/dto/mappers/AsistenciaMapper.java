package ledance.dto.mappers;

import ledance.dto.request.AsistenciaRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AsistenciaResponseDTO;
import ledance.dto.response.DisciplinaSimpleResponse;
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
                new AlumnoListadoResponse(
                        asistencia.getAlumno().getId(),
                        asistencia.getAlumno().getNombre(),
                        asistencia.getAlumno().getApellido()
                ),
                new DisciplinaSimpleResponse(
                        asistencia.getDisciplina().getId(),
                        asistencia.getDisciplina().getNombre()
                )
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
