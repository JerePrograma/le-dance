package ledance.servicios.disciplina;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.disciplina.DisciplinaHorarioMapper;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.disciplina.request.DisciplinaHorarioModificacionRequest;
import ledance.dto.profesor.ProfesorMapper;
import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.disciplina.request.DisciplinaHorarioRequest;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.disciplina.response.DisciplinaDetalleResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.dto.profesor.response.ProfesorListadoResponse;
import ledance.entidades.Disciplina;
import ledance.entidades.DisciplinaHorario;
import ledance.entidades.Profesor;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DisciplinaServicio implements IDisciplinaServicio {

    private static final Logger log = LoggerFactory.getLogger(DisciplinaServicio.class);

    private final DisciplinaRepositorio disciplinaRepositorio;
    private final ProfesorRepositorio profesorRepositorio;
    private final DisciplinaMapper disciplinaMapper;
    private final AlumnoMapper alumnoMapper;
    private final ProfesorMapper profesorMapper;
    private final DisciplinaHorarioServicio disciplinaHorarioServicio;

    public DisciplinaServicio(DisciplinaRepositorio disciplinaRepositorio,
                              ProfesorRepositorio profesorRepositorio,
                              DisciplinaMapper disciplinaMapper,
                              AlumnoMapper alumnoMapper,
                              ProfesorMapper profesorMapper,
                              InscripcionRepositorio inscripcionRepositorio,
                              AsistenciaMensualRepositorio asistenciaMensualRepositorio,
                              DisciplinaHorarioMapper disciplinaHorarioMapper,
                              DisciplinaHorarioServicio disciplinaHorarioServicio) {
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.profesorRepositorio = profesorRepositorio;
        this.disciplinaMapper = disciplinaMapper;
        this.alumnoMapper = alumnoMapper;
        this.profesorMapper = profesorMapper;
        this.disciplinaHorarioServicio = disciplinaHorarioServicio;
    }

    /**
     * Crea una nueva disciplina y, de ser proporcionados, delega la creación de sus horarios.
     */
    @Override
    @Transactional
    public DisciplinaDetalleResponse crearDisciplina(DisciplinaRegistroRequest request) {
        log.info("Iniciando creación de disciplina con nombre: {}", request.nombre());

        log.debug("Buscando profesor con id: {}", request.profesorId());
        Profesor profesor = profesorRepositorio.findById(request.profesorId())
                .orElseThrow(() -> new TratadorDeErrores.ProfesorNotFoundException(request.profesorId()));
        log.debug("Profesor encontrado: {} {}", profesor.getNombre(), profesor.getApellido());

        log.debug("Mapeando request a entidad Disciplina");
        Disciplina nuevaDisciplina = disciplinaMapper.toEntity(request);
        nuevaDisciplina.setProfesor(profesor);

        log.debug("Guardando disciplina en la base de datos");
        for (DisciplinaHorario horario : nuevaDisciplina.getHorarios()) {
            horario.setDisciplina(nuevaDisciplina);
        }
        nuevaDisciplina = disciplinaRepositorio.save(nuevaDisciplina);
        // Forzamos el flush para asegurarnos de que se genere el ID en la BD
        disciplinaRepositorio.flush();
        log.info("Disciplina creada con id: {}", nuevaDisciplina.getId());

        if (request.horarios() != null && !request.horarios().isEmpty()) {
            log.info("Se han recibido {} horarios para la disciplina id: {}",
                    request.horarios().size(), nuevaDisciplina.getId());
            List<DisciplinaHorario> nuevosHorarios =
                    disciplinaHorarioServicio.guardarHorarios(nuevaDisciplina.getId(), request.horarios());
            nuevaDisciplina.getHorarios().clear();
            nuevaDisciplina.getHorarios().addAll(nuevosHorarios);

            log.info("Se han asignado {} horarios a la disciplina id: {}",
                    nuevosHorarios.size(), nuevaDisciplina.getId());
        } else {
            log.info("No se recibieron horarios para la disciplina id: {}", nuevaDisciplina.getId());
        }
        DisciplinaDetalleResponse response = disciplinaMapper.toDetalleResponse(nuevaDisciplina);
        log.info("Respuesta de creación de disciplina preparada: {}", response);
        return response;
    }

    /**
     * Actualiza una disciplina existente y, si se incluyen horarios en el request, delega su gestión.
     */
    // DisciplinaServicio.java (método actualizarDisciplina)
    @Override
    @Transactional
    public DisciplinaDetalleResponse actualizarDisciplina(Long id, DisciplinaModificacionRequest request) {
        log.info("Iniciando actualización de disciplina con id: {}", id);
        Disciplina existente = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.DisciplinaNotFoundException(id));
        log.debug("Disciplina encontrada: {}", existente.getNombre());

        log.debug("Buscando profesor con id: {}", request.profesorId());
        Profesor profesor = profesorRepositorio.findById(request.profesorId())
                .orElseThrow(() -> new TratadorDeErrores.ProfesorNotFoundException(request.profesorId()));
        log.debug("Profesor encontrado: {} {}", profesor.getNombre(), profesor.getApellido());

        log.debug("Actualizando campos de la disciplina mediante el mapper");
        // Actualiza los campos básicos (sin horarios) mediante el mapper
        disciplinaMapper.updateEntityFromRequest(request, existente);
        existente.setProfesor(profesor);

        // Aquí actualizamos la colección de horarios de forma diferencial.
        // Se asume que el DTO de modificación contiene una lista de DisciplinaHorarioModificacionRequest
        if (request.horarios() != null) {
            log.info("Actualizando horarios para la disciplina id: {}", existente.getId());
            // Convertimos la lista de horarios recibida al tipo de modificación.
            // Si tu DTO en la request ya es de tipo DisciplinaHorarioModificacionRequest, se pasa directamente.
            List<DisciplinaHorarioModificacionRequest> horariosParaActualizar =
                    (List<DisciplinaHorarioModificacionRequest>)(Object) request.horarios();
            disciplinaHorarioServicio.actualizarHorarios(existente, horariosParaActualizar);
        }

        log.debug("Guardando disciplina actualizada en la base de datos");
        Disciplina disciplinaActualizada = disciplinaRepositorio.save(existente);
        DisciplinaDetalleResponse response = disciplinaMapper.toDetalleResponse(disciplinaActualizada);
        log.info("Disciplina actualizada correctamente con id: {}", disciplinaActualizada.getId());
        return response;
    }

    /**
     * Realiza una baja lógica de la disciplina.
     */
    @Override
    @Transactional
    public void eliminarDisciplina(Long id) {
        log.info("Iniciando baja lógica de la disciplina con id: {}", id);
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.DisciplinaNotFoundException(id));
        disciplina.setActivo(false);
        disciplinaRepositorio.save(disciplina);
        log.info("Disciplina con id: {} marcada como inactiva", id);
    }

    /**
     * Recupera el detalle de una disciplina por su ID.
     */
    @Override
    public DisciplinaDetalleResponse obtenerDisciplinaPorId(Long id) {
        log.info("Obteniendo detalle de la disciplina con id: {}", id);
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.DisciplinaNotFoundException(id));
        DisciplinaDetalleResponse response = disciplinaMapper.toDetalleResponse(disciplina);
        log.debug("Detalle obtenido: {}", response);
        return response;
    }

    /**
     * Obtiene las disciplinas que tienen clase en la fecha especificada.
     */
    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasPorFecha(String fecha) {
        log.info("Obteniendo disciplinas para la fecha: {}", fecha);
        LocalDate targetDate = LocalDate.parse(fecha);
        DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
        log.debug("DayOfWeek obtenido: {}", dayOfWeek);

        // Conversión de DayOfWeek a nuestro enum DiaSemana
        var diaSemana = switch (dayOfWeek) {
            case MONDAY -> ledance.entidades.DiaSemana.LUNES;
            case TUESDAY -> ledance.entidades.DiaSemana.MARTES;
            case WEDNESDAY -> ledance.entidades.DiaSemana.MIERCOLES;
            case THURSDAY -> ledance.entidades.DiaSemana.JUEVES;
            case FRIDAY -> ledance.entidades.DiaSemana.VIERNES;
            case SATURDAY -> ledance.entidades.DiaSemana.SABADO;
            case SUNDAY -> ledance.entidades.DiaSemana.DOMINGO;
        };
        log.debug("Convertido a DiaSemana: {}", diaSemana);

        List<?> horarios = disciplinaHorarioServicio.obtenerHorariosPorDia(diaSemana);
        log.debug("Se encontraron {} horarios para el día: {}", horarios.size(), diaSemana);

        List<Disciplina> disciplinas = ((List<ledance.entidades.DisciplinaHorario>) horarios).stream()
                .map(ledance.entidades.DisciplinaHorario::getDisciplina)
                .distinct()
                .collect(Collectors.toList());
        log.info("Total de disciplinas encontradas: {}", disciplinas.size());

        List<DisciplinaListadoResponse> response = disciplinas.stream()
                .map(disciplinaMapper::toListadoResponse)
                .collect(Collectors.toList());
        log.debug("Respuesta obtenida: {}", response);
        return response;
    }

    /**
     * Método aún no implementado para obtener disciplinas por un horario de inicio específico.
     */
    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasPorHorario(LocalTime horarioInicio) {
        log.warn("El método obtenerDisciplinasPorHorario no está implementado.");
        return List.of();
    }

    /**
     * Retorna los alumnos inscritos en una disciplina.
     */
    @Override
    public List<AlumnoListadoResponse> obtenerAlumnosDeDisciplina(Long disciplinaId) {
        log.info("Obteniendo alumnos inscritos para la disciplina con id: {}", disciplinaId);
        List<AlumnoListadoResponse> response = disciplinaRepositorio.findAlumnosPorDisciplina(disciplinaId).stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
        log.debug("Se encontraron {} alumnos inscritos", response.size());
        return response;
    }

    /**
     * Retorna el profesor asignado a una disciplina.
     */
    @Override
    public ProfesorListadoResponse obtenerProfesorDeDisciplina(Long disciplinaId) {
        log.info("Obteniendo profesor para la disciplina con id: {}", disciplinaId);
        Profesor profesor = disciplinaRepositorio.findProfesorPorDisciplina(disciplinaId)
                .orElseThrow(() -> new TratadorDeErrores.DisciplinaNotFoundException(disciplinaId));
        ProfesorListadoResponse response = profesorMapper.toListadoResponse(profesor);
        log.debug("Profesor obtenido: {} {}", response.nombre(), response.apellido());
        return response;
    }

    /**
     * Calcula y retorna las fechas en las que se dictan clases para una disciplina en un mes y año dados.
     */
    @Override
    public List<LocalDate> obtenerDiasClase(Long disciplinaId, Integer mes, Integer anio) {
        log.info("Calculando días de clase para la disciplina id: {} en {}/{}", disciplinaId, mes, anio);

        // Obtener los horarios como entidades (no DTOs)
        List<DisciplinaHorario> horarios = disciplinaHorarioServicio.obtenerHorariosEntidad(disciplinaId);

        // Convertir cada horario a un DayOfWeek utilizando el método toDayOfWeek() de tu enum
        Set<DayOfWeek> diasClase = horarios.stream()
                .map(h -> h.getDiaSemana().toDayOfWeek())
                .collect(Collectors.toSet());

        log.debug("Días de clase identificados: {}", diasClase);

        YearMonth yearMonth = YearMonth.of(anio, mes);
        List<LocalDate> fechasClase = new ArrayList<>();
        for (int dia = 1; dia <= yearMonth.lengthOfMonth(); dia++) {
            LocalDate fecha = LocalDate.of(anio, mes, dia);
            if (diasClase.contains(fecha.getDayOfWeek())) {
                fechasClase.add(fecha);
            }
        }
        log.info("Total de días de clase encontrados: {}", fechasClase.size());
        return fechasClase;
    }

    /**
     * Lista todas las disciplinas activas con detalle completo.
     */
    @Override
    public List<DisciplinaDetalleResponse> listarDisciplinas() {
        log.info("Listando todas las disciplinas activas (detalle completo)");
        List<DisciplinaDetalleResponse> response = disciplinaRepositorio.findByActivoTrue().stream()
                .map(disciplinaMapper::toDetalleResponse)
                .collect(Collectors.toList());
        log.debug("Total de disciplinas activas encontradas: {}", response.size());
        return response;
    }

    /**
     * Lista las disciplinas activas de forma simplificada.
     */
    @Override
    public List<DisciplinaListadoResponse> listarDisciplinasSimplificadas() {
        log.info("Listando todas las disciplinas activas (formato listado)");
        List<DisciplinaListadoResponse> response = disciplinaRepositorio.findByActivoTrue().stream()
                .map(disciplinaMapper::toListadoResponse)
                .collect(Collectors.toList());
        log.debug("Total de disciplinas activas encontradas: {}", response.size());
        return response;
    }

    /**
     * Busca disciplinas por nombre (parcial o completo) y retorna los resultados en formato listado.
     */
    @Override
    public List<DisciplinaListadoResponse> buscarPorNombre(String nombre) {
        log.info("Buscando disciplinas por nombre: {}", nombre);
        List<Disciplina> resultado = disciplinaRepositorio.buscarPorNombre(nombre);
        log.debug("Se encontraron {} disciplinas para el término: {}", resultado.size(), nombre);
        List<DisciplinaListadoResponse> response = resultado.stream()
                .map(disciplinaMapper::toListadoResponse)
                .collect(Collectors.toList());
        return response;
    }

    /**
     * Obtiene los horarios de una disciplina en formato de respuesta.
     */
    @Override
    public List<DisciplinaHorarioResponse> obtenerHorarios(Long disciplinaId) {
        log.info("Obteniendo horarios para la disciplina id: {}", disciplinaId);
        List<DisciplinaHorarioResponse> horarios = disciplinaHorarioServicio.obtenerHorarios(disciplinaId).stream()
                .map(h -> new DisciplinaHorarioResponse(
                        h.id(),
                        h.diaSemana(),
                        h.horarioInicio(),
                        h.duracion()
                ))
                .collect(Collectors.toList());
        log.debug("Se encontraron {} horarios para la disciplina id: {}", horarios.size(), disciplinaId);
        return horarios;
    }
}
