package ledance.servicios.profesor;

import ledance.dto.profesor.ProfesorMapper;
import ledance.dto.profesor.request.ProfesorModificacionRequest;
import ledance.dto.profesor.request.ProfesorRegistroRequest;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.dto.profesor.response.ProfesorDetalleResponse;
import ledance.dto.profesor.response.ProfesorListadoResponse;
import ledance.entidades.Disciplina;
import ledance.entidades.Profesor;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.ProfesorRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfesorServicio implements IProfesorServicio {

    private static final Logger log = LoggerFactory.getLogger(ProfesorServicio.class);

    private final ProfesorRepositorio profesorRepositorio;
    private final ProfesorMapper profesorMapper;

    public ProfesorServicio(ProfesorRepositorio profesorRepositorio, ProfesorMapper profesorMapper) {
        this.profesorRepositorio = profesorRepositorio;
        this.profesorMapper = profesorMapper;
    }

    /**
     * ✅ Registrar un nuevo profesor.
     */
    @Override
    @Transactional
    public ProfesorDetalleResponse registrarProfesor(ProfesorRegistroRequest request) {
        log.info("Registrando profesor: {} {}", request.nombre(), request.apellido());
        log.debug("Fecha de nacimiento: {}, Teléfono: {}", request.fechaNacimiento(), request.telefono());
        log.debug("Datos completos del profesor a registrar: {}", request); // Agregamos este log para depuración

        Profesor profesor = profesorMapper.toEntity(request);
        profesor.setEdad(calcularEdad(request.fechaNacimiento()));
        Profesor guardado = profesorRepositorio.save(profesor);
        return profesorMapper.toDetalleResponse(guardado);
    }

    @Override
    @Transactional
    public ProfesorDetalleResponse actualizarProfesor(Long id, ProfesorModificacionRequest request) {
        log.info("Actualizando profesor con id: {}", id);
        log.debug("Fecha de nacimiento: {}, Teléfono: {}", request.fechaNacimiento(), request.telefono()); // Agregamos este log para depuración
        log.debug("Datos completos del profesor a actualizar: {}", request); // Agregamos este log para depuración

        Profesor profesor = profesorRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));

        profesorMapper.updateEntityFromRequest(request, profesor);
        profesor.setEdad(calcularEdad(request.fechaNacimiento()));
        Profesor actualizado = profesorRepositorio.save(profesor);
        return profesorMapper.toDetalleResponse(actualizado);
    }

    /**
     * ✅ Obtener un profesor por ID.
     */
    @Override
    public ProfesorDetalleResponse obtenerProfesorPorId(Long id) {
        Profesor profesor = profesorRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));
        return profesorMapper.toDetalleResponse(profesor);
    }

    /**
     * ✅ Listar todos los profesores.
     */
    @Override
    public List<ProfesorListadoResponse> listarProfesores() {
        return profesorRepositorio.findAll().stream()
                .map(profesorMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Listar profesores activos.
     */
    @Override
    public List<ProfesorListadoResponse> listarProfesoresActivos() {
        return profesorRepositorio.findByActivoTrue().stream()
                .map(profesorMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Buscar profesores por nombre o apellido.
     */
    @Override
    public List<ProfesorListadoResponse> buscarPorNombre(String nombre) {
        return profesorRepositorio.findByNombreContainingOrApellidoContaining(nombre, nombre).stream()
                .map(profesorMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Eliminar un profesor (baja logica).
     */
    @Override
    @Transactional
    public void eliminarProfesor(Long id) {
        log.info("Eliminando profesor con id: {}", id);

        Profesor profesor = profesorRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));

        profesor.setActivo(false);
        profesorRepositorio.save(profesor);
    }

    /**
     * 🔄 Se actualiza automaticamente antes de persistir o actualizar
     */
    public int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento != null) {
            return Period.between(fechaNacimiento, LocalDate.now()).getYears();
        }
        return 0;
    }

    public List<DisciplinaListadoResponse> obtenerDisciplinasDeProfesor(Long profesorId) {
        Profesor profesor = profesorRepositorio.findById(profesorId)
                .orElseThrow(() -> new TratadorDeErrores.RecursoNoEncontradoException("Profesor no encontrado con id: " + profesorId));

        return profesor.getDisciplinas().stream()
                .map(this::toListadoResponse)
                .collect(Collectors.toList());
    }

    // Ejemplo simplificado en el método toListadoResponse:
    public DisciplinaListadoResponse toListadoResponse(Disciplina disciplina) {
        return new DisciplinaListadoResponse(
                disciplina.getId(),
                disciplina.getNombre(),
                disciplina.getHorarioInicio(),
                disciplina.getActivo(),
                disciplina.getProfesor().getId(),      // Agregado: id del profesor
                disciplina.getProfesor().getNombre()     // Agregado: nombre del profesor
        );
    }
}