package ledance.dto.mappers;

import ledance.dto.request.AlumnoRequest;
import ledance.dto.response.AlumnoResponse;
import ledance.entidades.Alumno;
import org.springframework.stereotype.Component;

@Component
public class AlumnoMapper {

    public Alumno toEntity(AlumnoRequest request) {
        Alumno alumno = new Alumno();
        alumno.setNombre(request.nombre());
        alumno.setFechaNacimiento(request.fechaNacimiento());
        alumno.setFechaIncorporacion(request.fechaIncorporacion());
        alumno.setCelular1(request.celular1());
        alumno.setCelular2(request.celular2());
        alumno.setTelefono(request.telefono());
        alumno.setEmail1(request.email1());
        alumno.setEmail2(request.email2());
        alumno.setDocumento(request.documento());
        alumno.setCuit(request.cuit());
        alumno.setNombrePadres(request.nombrePadres());
        alumno.setAutorizadoParaSalirSolo(Boolean.TRUE.equals(request.autorizadoParaSalirSolo()));
        alumno.setActivo(Boolean.TRUE.equals(request.activo()));
        alumno.setOtrasNotas(request.otrasNotas());
        alumno.setCuotaTotal(request.cuotaTotal());
        // "edad" se calculará en el servicio
        return alumno;
    }

    public void updateEntityFromRequest(Alumno alumno, AlumnoRequest request) {
        alumno.setNombre(request.nombre());
        alumno.setFechaNacimiento(request.fechaNacimiento());
        alumno.setFechaIncorporacion(request.fechaIncorporacion());
        alumno.setCelular1(request.celular1());
        alumno.setCelular2(request.celular2());
        alumno.setTelefono(request.telefono());
        alumno.setEmail1(request.email1());
        alumno.setEmail2(request.email2());
        alumno.setDocumento(request.documento());
        alumno.setCuit(request.cuit());
        alumno.setNombrePadres(request.nombrePadres());
        alumno.setAutorizadoParaSalirSolo(Boolean.TRUE.equals(request.autorizadoParaSalirSolo()));
        alumno.setActivo(Boolean.TRUE.equals(request.activo()));
        alumno.setOtrasNotas(request.otrasNotas());
        alumno.setCuotaTotal(request.cuotaTotal());
    }

    public AlumnoResponse toDTO(Alumno alumno) {
        return new AlumnoResponse(
                alumno.getId(),
                alumno.getNombre(),
                alumno.getApellido(),
                alumno.getFechaNacimiento(),
                alumno.getEdad(),
                alumno.getCelular1(),
                alumno.getCelular2(),
                alumno.getEmail1(),
                alumno.getEmail2(),
                alumno.getDocumento(),
                alumno.getCuit(),
                alumno.getFechaIncorporacion(),
                alumno.getNombrePadres(),
                alumno.getAutorizadoParaSalirSolo(),
                alumno.getActivo(),
                // disciplinasIds: ya NO las tenemos en la entidad. Podrías omitirlo o devolver null
                null
        );
    }
}
