package ledance.servicios;

import ledance.dto.request.ProfesorRegistroRequest;
import ledance.dto.response.DatosRegistroProfesorResponse;
import ledance.dto.response.ProfesorListadoResponse;
import ledance.entidades.Disciplina;
import ledance.entidades.Profesor;
import ledance.entidades.Usuario;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import ledance.repositorios.UsuarioRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfesorServicio {

    private final ProfesorRepositorio profesorRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    public ProfesorServicio(ProfesorRepositorio profesorRepositorio, DisciplinaRepositorio disciplinaRepositorio, UsuarioRepositorio usuarioRepositorio) {
        this.profesorRepositorio = profesorRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    public DatosRegistroProfesorResponse registrarProfesor(ProfesorRegistroRequest request) {
        Profesor profesor = new Profesor();
        profesor.setNombre(request.nombre());
        profesor.setApellido(request.apellido());
        profesor.setEspecialidad(request.especialidad());
        profesor.setAniosExperiencia(request.aniosExperiencia());
        profesor = profesorRepositorio.save(profesor);
        return new DatosRegistroProfesorResponse(profesor);
    }

    public DatosRegistroProfesorResponse obtenerProfesorPorId(Long id) {
        Profesor profesor = profesorRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));
        return new DatosRegistroProfesorResponse(profesor);
    }

    public List<DatosRegistroProfesorResponse> listarProfesores() {
        return profesorRepositorio.findAll().stream()
                .map(DatosRegistroProfesorResponse::new)
                .collect(Collectors.toList());
    }

    public void asignarUsuario(Long profesorId, Long usuarioId) {
        Profesor profesor = profesorRepositorio.findById(profesorId)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));

        Usuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Asignar el usuario al profesor
        profesor.setUsuario(usuario);

        // Guardar los cambios
        profesorRepositorio.save(profesor);
    }


    public void asignarDisciplina(Long profesorId, Long disciplinaId) {
        Profesor profesor = profesorRepositorio.findById(profesorId)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));
        Disciplina disciplina = disciplinaRepositorio.findById(disciplinaId)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada"));

        disciplina.setProfesor(profesor);
        disciplinaRepositorio.save(disciplina);
    }

    public List<ProfesorListadoResponse> listarProfesoresSimplificados() {
        return profesorRepositorio.findAll()
                .stream()
                .map(profesor -> new ProfesorListadoResponse(profesor.getId(), profesor.getNombre(), profesor.getApellido()))
                .collect(Collectors.toList());
    }

}
