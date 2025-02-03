package ledance.servicios;

import ledance.dto.mappers.DisciplinaMapper;
import ledance.dto.request.ProfesorRegistroRequest;
import ledance.dto.response.DatosRegistroProfesorResponse;
import ledance.dto.response.DisciplinaResponse;
import ledance.dto.response.ProfesorListadoResponse;
import ledance.dto.mappers.ProfesorMapper;
import ledance.entidades.Profesor;
import ledance.entidades.Usuario;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import ledance.repositorios.UsuarioRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfesorServicio implements IProfesorServicio {

    private static final Logger log = LoggerFactory.getLogger(ProfesorServicio.class);

    private final ProfesorRepositorio profesorRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ProfesorMapper profesorMapper;
    private final DisciplinaMapper disciplinaMapper;

    public ProfesorServicio(ProfesorRepositorio profesorRepositorio,
                            DisciplinaRepositorio disciplinaRepositorio,
                            UsuarioRepositorio usuarioRepositorio,
                            ProfesorMapper profesorMapper, DisciplinaMapper disciplinaMapper) {
        this.profesorRepositorio = profesorRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.profesorMapper = profesorMapper;
        this.disciplinaMapper = disciplinaMapper;
    }

    @Override
    @Transactional
    public DatosRegistroProfesorResponse registrarProfesor(ProfesorRegistroRequest request) {
        log.info("Registrando profesor: {} {}", request.nombre(), request.apellido());
        Profesor profesor = profesorMapper.toEntity(request);
        Profesor guardado = profesorRepositorio.save(profesor);
        return profesorMapper.toDatosRegistroDTO(guardado);
    }

    @Override
    public DatosRegistroProfesorResponse obtenerProfesorPorId(Long id) {
        Profesor profesor = profesorRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));
        return profesorMapper.toDatosRegistroDTO(profesor);
    }

    @Override
    public List<DatosRegistroProfesorResponse> listarProfesores() {
        return profesorRepositorio.findAll().stream()
                .map(profesorMapper::toDatosRegistroDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void asignarUsuario(Long profesorId, Long usuarioId) {
        Profesor profesor = profesorRepositorio.findById(profesorId)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));
        Usuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        profesor.setUsuario(usuario);
        profesorRepositorio.save(profesor);
    }

    @Override
    @Transactional
    public void asignarDisciplina(Long profesorId, Long disciplinaId) {
        Profesor profesor = profesorRepositorio.findById(profesorId)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));
        var disciplina = disciplinaRepositorio.findById(disciplinaId)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        disciplina.setProfesor(profesor);
        disciplinaRepositorio.save(disciplina);
    }

    @Override
    public List<ProfesorListadoResponse> listarProfesoresSimplificados() {
        return profesorRepositorio.findAll().stream()
                .map(profesorMapper::toListadoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DisciplinaResponse> obtenerDisciplinasDeProfesor(Long profesorId) {
        Profesor profesor = profesorRepositorio.findById(profesorId)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));
        return profesor.getDisciplinas().stream()
                .map(disciplinaMapper::toDTO)
                .collect(Collectors.toList());
    }

}
