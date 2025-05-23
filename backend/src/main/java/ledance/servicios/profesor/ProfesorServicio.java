package ledance.servicios.profesor;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.profesor.ProfesorMapper;
import ledance.dto.profesor.request.ProfesorModificacionRequest;
import ledance.dto.profesor.request.ProfesorRegistroRequest;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.profesor.response.ProfesorResponse;
import ledance.dto.reporte.request.ReporteLiquidacionRequest;
import ledance.entidades.Alumno;
import ledance.entidades.Profesor;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.DisciplinaHorarioRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import jakarta.transaction.Transactional;
import ledance.servicios.pdfs.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfesorServicio implements IProfesorServicio {

    private static final Logger log = LoggerFactory.getLogger(ProfesorServicio.class);

    private final ProfesorRepositorio profesorRepositorio;
    private final ProfesorMapper profesorMapper;
    private final DisciplinaMapper disciplinaMapper;
    private final AlumnoMapper alumnoMapper;
    private final PdfService pdfService;

    public ProfesorServicio(ProfesorRepositorio profesorRepositorio, ProfesorMapper profesorMapper, DisciplinaHorarioRepositorio disciplinaHorarioRepositorio, DisciplinaMapper disciplinaMapper, AlumnoMapper alumnoMapper, PdfService pdfService) {
        this.profesorRepositorio = profesorRepositorio;
        this.profesorMapper = profesorMapper;
        this.disciplinaMapper = disciplinaMapper;
        this.alumnoMapper = alumnoMapper;
        this.pdfService = pdfService;
    }

    /**
     * ✅ Registrar un nuevo profesor.
     */
    @Override
    @Transactional
    public ProfesorResponse registrarProfesor(ProfesorRegistroRequest request) {
        log.info("Registrando profesor: {} {}", request.nombre(), request.apellido());
        log.info("Fecha de nacimiento: {}, Telefono: {}", request.fechaNacimiento(), request.telefono());
        log.info("Datos completos del profesor a registrar: {}", request); // Agregamos este log para depuracion

        Profesor profesor = profesorMapper.toEntity(request);
        profesor.setEdad(calcularEdad(request.fechaNacimiento()));
        Profesor guardado = profesorRepositorio.save(profesor);
        return profesorMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public ProfesorResponse actualizarProfesor(Long id, ProfesorModificacionRequest request) {
        log.info("Actualizando profesor con id: {}", id);
        log.info("Fecha de nacimiento: {}, Telefono: {}", request.fechaNacimiento(), request.telefono()); // Agregamos este log para depuracion
        log.info("Datos completos del profesor a actualizar: {}", request); // Agregamos este log para depuracion

        Profesor profesor = profesorRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));

        profesorMapper.updateEntityFromRequest(request, profesor);
        profesor.setEdad(calcularEdad(request.fechaNacimiento()));
        Profesor actualizado = profesorRepositorio.save(profesor);
        return profesorMapper.toResponse(actualizado);
    }

    /**
     * ✅ Obtener un profesor por ID.
     */
    @Override
    public ProfesorResponse obtenerProfesorPorId(Long id) {
        Profesor profesor = profesorRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado."));
        return profesorMapper.toResponse(profesor);
    }

    /**
     * ✅ Listar todos los profesores.
     */
    @Override
    public List<ProfesorResponse> listarProfesores() {
        return profesorRepositorio.findAll().stream()
                .map(profesorMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Listar profesores activos.
     */
    @Override
    public List<ProfesorResponse> listarProfesoresActivos() {
        return profesorRepositorio.findByActivoTrue().stream()
                .map(profesorMapper::toResponse)
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

    public List<DisciplinaResponse> obtenerDisciplinasDeProfesor(Long profesorId) {
        Profesor profesor = profesorRepositorio.findById(profesorId)
                .orElseThrow(() -> new TratadorDeErrores.RecursoNoEncontradoException("Profesor no encontrado con id: " + profesorId));

        return profesor.getDisciplinas().stream()
                .map(disciplinaMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProfesorResponse> buscarPorNombre(String nombre) {
        return profesorRepositorio.buscarPorNombreCompleto(nombre).stream()
                .map(profesorMapper::toResponse)
                .collect(Collectors.toList());
    }


    /**
     * Retorna los alumnos que están inscriptos en cualquiera de las disciplinas del profesor.
     */
    public List<AlumnoResponse> obtenerAlumnosDeProfesor(Long profesorId) {
        List<Alumno> alumnos = profesorRepositorio.findAlumnosPorProfesor(profesorId);
        return alumnos.stream()
                .map(alumnoMapper::toResponse)
                .collect(Collectors.toList());
    }

    public byte[] exportarLiquidacionProfesor(ReporteLiquidacionRequest req) throws IOException {
        // simplemente delega el PDF, con la lista de DetallePagoResponse ya viniendo del frontend
        return pdfService.generarLiquidacionProfesorPdf(
                req.profesor(),
                req.disciplina(),
                req.fechaInicio(),
                req.detalles(),
                req.porcentaje()
        );
    }
}