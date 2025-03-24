package ledance.servicios.disciplina;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.disciplina.DisciplinaHorarioMapper;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.profesor.ProfesorMapper;
import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.dto.profesor.response.ProfesorResponse;
import ledance.entidades.*;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.inscripcion.InscripcionServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final DisciplinaHorarioRepositorio disciplinaHorarioRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final AsistenciaAlumnoMensualRepositorio asistenciaAlumnoMensualRepositorio;
    private final AsistenciaDiariaRepositorio asistenciaDiariaRepositorio;
    private final SalonRepositorio salonRepositorio;
    private final MensualidadRepositorio mensualidadRepositorio;
    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;

    public DisciplinaServicio(DisciplinaRepositorio disciplinaRepositorio,
                              ProfesorRepositorio profesorRepositorio,
                              DisciplinaMapper disciplinaMapper,
                              AlumnoMapper alumnoMapper,
                              ProfesorMapper profesorMapper,
                              DisciplinaHorarioServicio disciplinaHorarioServicio,
                              DisciplinaHorarioRepositorio disciplinaHorarioRepositorio, InscripcionRepositorio inscripcionRepositorio, AsistenciaAlumnoMensualRepositorio asistenciaAlumnoMensualRepositorio, AsistenciaDiariaRepositorio asistenciaDiariaRepositorio, SalonRepositorio salonRepositorio, MensualidadRepositorio mensualidadRepositorio, AsistenciaMensualRepositorio asistenciaMensualRepositorio) {
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.profesorRepositorio = profesorRepositorio;
        this.disciplinaMapper = disciplinaMapper;
        this.alumnoMapper = alumnoMapper;
        this.profesorMapper = profesorMapper;
        this.disciplinaHorarioServicio = disciplinaHorarioServicio;
        this.disciplinaHorarioRepositorio = disciplinaHorarioRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.asistenciaAlumnoMensualRepositorio = asistenciaAlumnoMensualRepositorio;
        this.asistenciaDiariaRepositorio = asistenciaDiariaRepositorio;
        this.salonRepositorio = salonRepositorio;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
    }

    /**
     * Crea una nueva disciplina y, de ser proporcionados, delega la creacion de sus horarios.
     */
    @Override
    @Transactional
    public DisciplinaResponse crearDisciplina(DisciplinaRegistroRequest request) {
        log.info("Iniciando creacion de disciplina con nombre: {}", request.nombre());

        log.info("Buscando profesor con id: {}", request.profesorId());
        Profesor profesor = profesorRepositorio.findById(request.profesorId())
                .orElseThrow(() -> new TratadorDeErrores.ProfesorNotFoundException(request.profesorId()));
        log.info("Profesor encontrado: {} {}", profesor.getNombre(), profesor.getApellido());

        log.info("Mapeando request a entidad Disciplina");
        Disciplina nuevaDisciplina = disciplinaMapper.toEntity(request);
        nuevaDisciplina.setProfesor(profesor);

        log.info("Guardando disciplina en la base de datos");
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
        DisciplinaResponse response = disciplinaMapper.toResponse(nuevaDisciplina);
        log.info("Respuesta de creacion de disciplina preparada: {}", response);
        return response;
    }

    /**
     * Actualiza una disciplina existente y, si se incluyen horarios en el request, delega su gestion.
     */
    // DisciplinaServicio.java (metodo actualizarDisciplina)
    @Override
    @Transactional
    public DisciplinaResponse actualizarDisciplina(Long id, DisciplinaModificacionRequest request) {
        log.info("Iniciando actualizacion de disciplina con id: {}", id);

        // Recupera la disciplina existente
        Disciplina existente = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.DisciplinaNotFoundException(id));
        log.info("Disciplina encontrada: {}", existente.getNombre());

        // Recupera el profesor indicado en la request
        Profesor profesor = profesorRepositorio.findById(request.profesorId())
                .orElseThrow(() -> new TratadorDeErrores.ProfesorNotFoundException(request.profesorId()));
        log.info("Profesor encontrado: {} {}", profesor.getNombre(), profesor.getApellido());

        // Actualiza los campos básicos (excepto el salón, que se maneja manualmente)
        disciplinaMapper.updateEntityFromRequest(request, existente);
        existente.setProfesor(profesor);

        // Recupera el salón administrado usando el id del request y lo asigna
        Salon salon = salonRepositorio.findById(request.salonId())
                .orElseThrow(() -> new TratadorDeErrores.ResourceNotFoundException(request.salonId().toString()));
        log.info("Salón encontrado: {}", salon.getNombre());
        existente.setSalon(salon);

        // Actualiza o elimina la colección de horarios
        if (request.horarios() == null || request.horarios().isEmpty()) {
            log.info("Eliminando todos los horarios para la disciplina con id: {}", existente.getId());
            existente.getHorarios().clear();
        } else {
            log.info("Actualizando horarios para la disciplina con id: {}", existente.getId());
            disciplinaHorarioServicio.actualizarHorarios(existente, request.horarios(), LocalDate.now());
        }

        // Guarda la disciplina actualizada
        Disciplina disciplinaActualizada = disciplinaRepositorio.save(existente);
        log.info("Disciplina actualizada correctamente con id: {}", disciplinaActualizada.getId());

        return disciplinaMapper.toResponse(disciplinaActualizada);
    }

    /**
     * Realiza una baja logica de la disciplina.
     */
    @Transactional
    public void eliminarDisciplina(Long id) {
        // Recupera la disciplina
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Disciplina no encontrada"));

        // 1. Procesar las inscripciones de la disciplina
        if (disciplina.getInscripciones() != null && !disciplina.getInscripciones().isEmpty()) {
            // Se usa una copia para evitar ConcurrentModificationException
            List<Inscripcion> inscripciones = new ArrayList<>(disciplina.getInscripciones());
            for (Inscripcion inscripcion : inscripciones) {
                // 1.1. Procesar las asistencias de alumno asociadas a la inscripción
                List<AsistenciaAlumnoMensual> asistenciasAlumno =
                        asistenciaAlumnoMensualRepositorio.findByInscripcionId(inscripcion.getId());
                if (asistenciasAlumno != null && !asistenciasAlumno.isEmpty()) {
                    List<AsistenciaAlumnoMensual> copiaAsistencias = new ArrayList<>(asistenciasAlumno);
                    for (AsistenciaAlumnoMensual asistenciaAlumno : copiaAsistencias) {
                        // 1.1.1. Eliminar las asistencias diarias asociadas
                        List<AsistenciaDiaria> asistenciasDiarias =
                                asistenciaDiariaRepositorio.findByAsistenciaAlumnoMensualId(asistenciaAlumno.getId());
                        if (asistenciasDiarias != null && !asistenciasDiarias.isEmpty()) {
                            asistenciaDiariaRepositorio.deleteAll(new ArrayList<>(asistenciasDiarias));
                        }
                        // 1.1.2. Eliminar el registro de AsistenciaAlumnoMensual
                        asistenciaAlumnoMensualRepositorio.delete(asistenciaAlumno);
                    }
                }
                // 1.2. Eliminar las mensualidades asociadas a la inscripción
                if (inscripcion.getMensualidades() != null && !inscripcion.getMensualidades().isEmpty()) {
                    List<Mensualidad> mensualidades = new ArrayList<>(inscripcion.getMensualidades());
                    for (Mensualidad mensualidad : mensualidades) {
                        // Removemos la referencia en la colección para evitar que Hibernate intente actualizarla
                        inscripcion.getMensualidades().remove(mensualidad);
                        // Eliminamos directamente la mensualidad
                        mensualidadRepositorio.delete(mensualidad);
                    }
                    mensualidadRepositorio.flush();
                }
                // 1.3. Finalmente, eliminar la inscripción
                inscripcionRepositorio.delete(inscripcion);
            }
            inscripcionRepositorio.flush();
        }

        // 2. Eliminar las asistencias mensuales asociadas a la disciplina
        List<AsistenciaMensual> asistenciasMensuales = asistenciaMensualRepositorio.findByDisciplinaId(disciplina.getId());
        if (asistenciasMensuales != null && !asistenciasMensuales.isEmpty()) {
            asistenciaMensualRepositorio.deleteAll(new ArrayList<>(asistenciasMensuales));
            asistenciaMensualRepositorio.flush();
        }

        // 3. Eliminar los horarios asociados
        if (disciplina.getHorarios() != null && !disciplina.getHorarios().isEmpty()) {
            // Creamos una copia para iterar y remover sin modificar la lista original
            List<DisciplinaHorario> horarios = new ArrayList<>(disciplina.getHorarios());
            for (DisciplinaHorario horario : horarios) {
                // Removemos el horario de la colección para evitar que se intente actualizar
                disciplina.getHorarios().remove(horario);
                // Eliminamos el registro directamente
                disciplinaHorarioRepositorio.delete(horario);
            }
            disciplinaHorarioRepositorio.flush();
        }

        // 4. Finalmente, eliminar la disciplina
        disciplinaRepositorio.delete(disciplina);
    }

    /**
     * Recupera el detalle de una disciplina por su ID.
     */
    @Override
    public DisciplinaResponse obtenerDisciplinaPorId(Long id) {
        log.info("Obteniendo detalle de la disciplina con id: {}", id);
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.DisciplinaNotFoundException(id));
        DisciplinaResponse response = disciplinaMapper.toResponse(disciplina);
        log.info("Detalle obtenido: {}", response);
        return response;
    }

    /**
     * Obtiene las disciplinas que tienen clase en la fecha especificada.
     */
    @Override
    public List<DisciplinaResponse> obtenerDisciplinasPorFecha(String fecha) {
        log.info("Obteniendo disciplinas para la fecha: {}", fecha);
        LocalDate targetDate = LocalDate.parse(fecha);
        DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
        log.info("DayOfWeek obtenido: {}", dayOfWeek);

        // Conversion de DayOfWeek a nuestro enum DiaSemana
        var diaSemana = switch (dayOfWeek) {
            case MONDAY -> ledance.entidades.DiaSemana.LUNES;
            case TUESDAY -> ledance.entidades.DiaSemana.MARTES;
            case WEDNESDAY -> ledance.entidades.DiaSemana.MIERCOLES;
            case THURSDAY -> ledance.entidades.DiaSemana.JUEVES;
            case FRIDAY -> ledance.entidades.DiaSemana.VIERNES;
            case SATURDAY -> ledance.entidades.DiaSemana.SABADO;
            case SUNDAY -> ledance.entidades.DiaSemana.DOMINGO;
        };
        log.info("Convertido a DiaSemana: {}", diaSemana);

        List<?> horarios = disciplinaHorarioServicio.obtenerHorariosPorDia(diaSemana);
        log.info("Se encontraron {} horarios para el dia: {}", horarios.size(), diaSemana);

        List<Disciplina> disciplinas = ((List<ledance.entidades.DisciplinaHorario>) horarios).stream()
                .map(ledance.entidades.DisciplinaHorario::getDisciplina)
                .distinct()
                .collect(Collectors.toList());
        log.info("Total de disciplinas encontradas: {}", disciplinas.size());

        List<DisciplinaResponse> response = disciplinas.stream()
                .map(disciplinaMapper::toResponse)
                .collect(Collectors.toList());
        log.info("Respuesta obtenida: {}", response);
        return response;
    }

    /**
     * Metodo aun no implementado para obtener disciplinas por un horario de inicio especifico.
     */
    @Override
    public List<DisciplinaResponse> obtenerDisciplinasPorHorario(LocalTime horarioInicio) {
        log.warn("El metodo obtenerDisciplinasPorHorario no esta implementado.");
        return List.of();
    }

    /**
     * Retorna los alumnos inscritos en una disciplina.
     */
    @Override
    public List<AlumnoResponse> obtenerAlumnosDeDisciplina(Long disciplinaId) {
        log.info("Obteniendo alumnos inscritos para la disciplina con id: {}", disciplinaId);
        List<AlumnoResponse> response = disciplinaRepositorio.findAlumnosPorDisciplina(disciplinaId).stream()
                .map(alumnoMapper::toResponse)
                .collect(Collectors.toList());
        log.info("Se encontraron {} alumnos inscritos", response.size());
        return response;
    }

    /**
     * Retorna el profesor asignado a una disciplina.
     */
    @Override
    public ProfesorResponse obtenerProfesorDeDisciplina(Long disciplinaId) {
        log.info("Obteniendo profesor para la disciplina con id: {}", disciplinaId);
        Profesor profesor = disciplinaRepositorio.findProfesorPorDisciplina(disciplinaId)
                .orElseThrow(() -> new TratadorDeErrores.DisciplinaNotFoundException(disciplinaId));
        ProfesorResponse response = profesorMapper.toResponse(profesor);
        log.info("Profesor obtenido: {} {}", response.nombre(), response.apellido());
        return response;
    }

    /**
     * Calcula y retorna las fechas en las que se dictan clases para una disciplina en un mes y año dados.
     */
    @Override
    public List<LocalDate> obtenerDiasClase(Long disciplinaId, Integer mes, Integer anio) {
        log.info("Calculando dias de clase para la disciplina id: {} en {}/{}", disciplinaId, mes, anio);

        // Obtener los horarios como entidades (no DTOs)
        List<DisciplinaHorario> horarios = disciplinaHorarioServicio.obtenerHorariosEntidad(disciplinaId);

        // Convertir cada horario a un DayOfWeek utilizando el metodo toDayOfWeek() de tu enum
        Set<DayOfWeek> diasClase = horarios.stream()
                .map(h -> h.getDiaSemana().toDayOfWeek())
                .collect(Collectors.toSet());

        log.info("Dias de clase identificados: {}", diasClase);

        YearMonth yearMonth = YearMonth.of(anio, mes);
        List<LocalDate> fechasClase = new ArrayList<>();
        for (int dia = 1; dia <= yearMonth.lengthOfMonth(); dia++) {
            LocalDate fecha = LocalDate.of(anio, mes, dia);
            if (diasClase.contains(fecha.getDayOfWeek())) {
                fechasClase.add(fecha);
            }
        }
        log.info("Total de dias de clase encontrados: {}", fechasClase.size());
        return fechasClase;
    }

    /**
     * Lista todas las disciplinas activas con detalle completo.
     */
    @Override
    public List<DisciplinaResponse> listarDisciplinas() {
        log.info("Listando todas las disciplinas activas (detalle completo)");
        List<DisciplinaResponse> response = disciplinaRepositorio.findByActivoTrue().stream()
                .map(disciplinaMapper::toResponse)
                .collect(Collectors.toList());
        log.info("Total de disciplinas activas encontradas: {}", response.size());
        return response;
    }

    /**
     * Lista las disciplinas activas de forma simplificada.
     */
    @Override
    public List<DisciplinaResponse> listarDisciplinasSimplificadas() {
        log.info("Listando todas las disciplinas activas (formato listado)");
        List<DisciplinaResponse> response = disciplinaRepositorio.findByActivoTrue().stream()
                .map(disciplinaMapper::toResponse)
                .collect(Collectors.toList());
        log.info("Total de disciplinas activas encontradas: {}", response.size());
        return response;
    }

    /**
     * Busca disciplinas por nombre (parcial o completo) y retorna los resultados en formato listado.
     */
    @Override
    public List<DisciplinaResponse> buscarPorNombre(String nombre) {
        log.info("Buscando disciplinas por nombre: {}", nombre);
        List<Disciplina> resultado = disciplinaRepositorio.buscarPorNombre(nombre);
        log.info("Se encontraron {} disciplinas para el termino: {}", resultado.size(), nombre);
        List<DisciplinaResponse> response = resultado.stream()
                .map(disciplinaMapper::toResponse)
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
        log.info("Se encontraron {} horarios para la disciplina id: {}", horarios.size(), disciplinaId);
        return horarios;
    }

    public void darBajaDisciplina(Long id) {
        log.info("Iniciando baja logica de la disciplina con id: {}", id);
        Disciplina disciplina = disciplinaRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.DisciplinaNotFoundException(id));
        disciplina.setActivo(false);
        disciplinaRepositorio.save(disciplina);
        log.info("Disciplina con id: {} marcada como inactiva", id);
    }
}
