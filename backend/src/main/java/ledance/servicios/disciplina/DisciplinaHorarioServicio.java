package ledance.servicios.disciplina;

import ledance.dto.disciplina.DisciplinaHorarioMapper;
import ledance.dto.disciplina.request.DisciplinaHorarioModificacionRequest;
import ledance.dto.disciplina.request.DisciplinaHorarioRequest;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.entidades.DiaSemana;
import ledance.entidades.Disciplina;
import ledance.entidades.DisciplinaHorario;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.DisciplinaHorarioRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import jakarta.transaction.Transactional;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DisciplinaHorarioServicio {

    private static final Logger log = LoggerFactory.getLogger(DisciplinaHorarioServicio.class);

    private final DisciplinaHorarioRepositorio disciplinaHorarioRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final DisciplinaHorarioMapper disciplinaHorarioMapper;
    private final AsistenciaMensualServicio asistenciaMensualServicio;

    public DisciplinaHorarioServicio(
            DisciplinaHorarioRepositorio disciplinaHorarioRepositorio,
            DisciplinaRepositorio disciplinaRepositorio,
            DisciplinaHorarioMapper disciplinaHorarioMapper, AsistenciaMensualServicio asistenciaMensualServicio) {
        this.disciplinaHorarioRepositorio = disciplinaHorarioRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.disciplinaHorarioMapper = disciplinaHorarioMapper;
        this.asistenciaMensualServicio = asistenciaMensualServicio;
    }

    /**
     * Guarda los horarios para una disciplina.
     * Primero elimina los horarios existentes y luego guarda los nuevos.
     */
    @Transactional
    public List<DisciplinaHorario> guardarHorarios(Long disciplinaId, List<DisciplinaHorarioRequest> horariosRequest) {
        log.info("Guardando {} horarios para disciplina id: {}", horariosRequest.size(), disciplinaId);

        // Recupera la disciplina
        Disciplina disciplina = disciplinaRepositorio.findById(disciplinaId)
                .orElseThrow(() -> new TratadorDeErrores.DisciplinaNotFoundException(disciplinaId));
        log.debug("Disciplina recuperada: id={}, nombre={}", disciplina.getId(), disciplina.getNombre());

        // Elimina los horarios previos asociados a la disciplina
        log.debug("Eliminando horarios previos para disciplina id: {}", disciplinaId);
        disciplinaHorarioRepositorio.deleteByDisciplinaId(disciplinaId);

        // Mapear cada request a entidad y asignarle la disciplina
        List<DisciplinaHorario> nuevosHorarios = horariosRequest.stream()
                .map(req -> {
                    DisciplinaHorario horario = disciplinaHorarioMapper.toEntity(req);
                    // Asignación exclusiva en el servicio:
                    horario.setDisciplina(disciplina);
                    log.debug("Horario mapeado: dia={}, inicio={}, duracion={}, disciplina asignada: {}",
                            horario.getDiaSemana(), horario.getHorarioInicio(), horario.getDuracion(),
                            (horario.getDisciplina() != null ? horario.getDisciplina().getId() : "null"));
                    return horario;
                })
                .collect(Collectors.toList());
        log.info("Mapeados {} horarios para guardar", nuevosHorarios.size());

        // Guarda todos los nuevos horarios y actualiza la colección en la disciplina
        List<DisciplinaHorario> savedHorarios = disciplinaHorarioRepositorio.saveAll(nuevosHorarios);
        log.info("Se guardaron {} horarios para disciplina id: {}", savedHorarios.size(), disciplinaId);
        disciplina.getHorarios().clear();
        disciplina.getHorarios().addAll(savedHorarios);

        log.debug("Colección de horarios actualizada en la disciplina: {}", disciplina.getHorarios());

        return savedHorarios;
    }

    // DisciplinaHorarioServicio.java
    @Transactional
    public List<DisciplinaHorario> actualizarHorarios(Disciplina disciplina,
                                                      List<DisciplinaHorarioModificacionRequest> horariosRequest) {

        log.info("Actualizando {} horarios para disciplina id: {}", horariosRequest.size(), disciplina.getId());

        // Obtén la lista actual de horarios asociados (suponiendo que la colección ya está cargada)
        List<DisciplinaHorario> existentes = new ArrayList<>(disciplina.getHorarios());

        // Mapea los horarios existentes por su ID
        Map<Long, DisciplinaHorario> existentesMap = existentes.stream()
                .filter(h -> h.getId() != null)
                .collect(Collectors.toMap(DisciplinaHorario::getId, h -> h));

        List<DisciplinaHorario> updatedHorarios = new ArrayList<>();

        // Procesa cada DTO recibido
        for (DisciplinaHorarioModificacionRequest dto : horariosRequest) {
            if (dto.id() != null && existentesMap.containsKey(dto.id())) {
                // Actualiza el horario existente
                DisciplinaHorario horarioExistente = existentesMap.get(dto.id());
                horarioExistente.setDiaSemana(dto.diaSemana());
                horarioExistente.setHorarioInicio(dto.horarioInicio());
                horarioExistente.setDuracion(dto.duracion());
                updatedHorarios.add(horarioExistente);
                // Remueve del mapa para identificar los que se eliminarán
                existentesMap.remove(dto.id());
            } else {
                // Si no tiene id, se trata como nuevo
                DisciplinaHorario nuevo = disciplinaHorarioMapper.toEntity(
                        new DisciplinaHorarioRequest(dto.diaSemana(), dto.horarioInicio(), dto.duracion())
                );
                nuevo.setDisciplina(disciplina);
                updatedHorarios.add(nuevo);
            }
        }

        // Los horarios que queden en existentesMap son los eliminados en la request
        for (DisciplinaHorario eliminado : existentesMap.values()) {
            disciplina.getHorarios().remove(eliminado);
            disciplinaHorarioRepositorio.delete(eliminado);
        }

        // Actualiza la colección de la Disciplina sin reemplazar la instancia
        disciplina.getHorarios().clear();
        disciplina.getHorarios().addAll(updatedHorarios);
        List<DisciplinaHorario> savedHorarios = disciplinaHorarioRepositorio.saveAll(updatedHorarios);
        log.info("Actualizados {} horarios para disciplina id: {}", savedHorarios.size(), disciplina.getId());

        // INVOCAMOS la actualización de asistencias a partir de la fecha del cambio.
        // Aquí usamos LocalDate.now() como fecha de cambio; podrías recibirla como parámetro si es necesario.
        asistenciaMensualServicio.actualizarAsistenciasPorCambioHorario(disciplina.getId(), LocalDate.now());

        return savedHorarios;
    }

    /**
     * Obtiene los horarios asociados a una disciplina y los retorna en formato de respuesta.
     */
    public List<DisciplinaHorarioResponse> obtenerHorarios(Long disciplinaId) {
        log.info("Obteniendo horarios para disciplina id: {}", disciplinaId);
        List<DisciplinaHorario> horarios = disciplinaHorarioRepositorio.findByDisciplinaId(disciplinaId);
        log.debug("Se encontraron {} horarios", horarios.size());
        List<DisciplinaHorarioResponse> response = disciplinaHorarioMapper.toResponseList(horarios);
        log.debug("Mapeo de horarios completado, retornando respuesta");
        return response;
    }

    /**
     * Elimina todos los horarios asociados a una disciplina.
     */
    @Transactional
    public void eliminarHorarios(Long disciplinaId) {
        log.info("Eliminando todos los horarios para disciplina id: {}", disciplinaId);
        disciplinaHorarioRepositorio.deleteByDisciplinaId(disciplinaId);
        log.debug("Horarios eliminados para disciplina id: {}", disciplinaId);
    }

    /**
     * Obtiene los horarios que corresponden a un día específico.
     */
    public List<DisciplinaHorario> obtenerHorariosPorDia(DiaSemana diaSemana) {
        log.info("Obteniendo horarios para el día: {}", diaSemana);
        List<DisciplinaHorario> horarios = disciplinaHorarioRepositorio.findByDiaSemana(diaSemana);
        log.debug("Se encontraron {} horarios para el día {}", horarios.size(), diaSemana);
        return horarios;
    }

    @Transactional
    public List<DisciplinaHorario> obtenerHorariosEntidad(Long disciplinaId) {
        // Este método usa el repositorio y retorna las entidades directamente
        return disciplinaHorarioRepositorio.findByDisciplinaId(disciplinaId);
    }

}
