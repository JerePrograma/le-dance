package ledance.servicios;

import ledance.dto.request.InscripcionRequest;
import ledance.dto.response.InscripcionResponse;
import ledance.dto.mappers.InscripcionMapper;
import ledance.entidades.Alumno;
import ledance.entidades.Bonificacion;
import ledance.entidades.Disciplina;
import ledance.entidades.Inscripcion;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.BonificacionRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InscripcionServicio implements IInscripcionServicio {

    private static final Logger log = LoggerFactory.getLogger(InscripcionServicio.class);

    private final InscripcionRepositorio inscripcionRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;
    private final InscripcionMapper inscripcionMapper;

    public InscripcionServicio(InscripcionRepositorio inscripcionRepositorio,
                               AlumnoRepositorio alumnoRepositorio,
                               DisciplinaRepositorio disciplinaRepositorio,
                               BonificacionRepositorio bonificacionRepositorio,
                               InscripcionMapper inscripcionMapper) {
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.inscripcionMapper = inscripcionMapper;
    }

    @Override
    @Transactional
    public InscripcionResponse crearInscripcion(InscripcionRequest request) {
        log.info("Creando inscripcion para alumnoId: {} en disciplinaId: {}", request.alumnoId(), request.disciplinaId());

        Alumno alumno = alumnoRepositorio.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        Disciplina disciplina = disciplinaRepositorio.findById(request.disciplinaId())
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));

        Bonificacion bonif = null;
        Double costoFinal = request.costoParticular();

        if (request.bonificacionId() != null) {
            bonif = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));
            if (bonif.getPorcentajeDescuento() != null && bonif.getPorcentajeDescuento() > 0) {
                costoFinal = costoFinal - (costoFinal * bonif.getPorcentajeDescuento() / 100);
            }
        }

        Inscripcion inscripcion = inscripcionMapper.toEntity(request);
        // Asignar asociaciones manualmente
        inscripcion.setAlumno(alumno);
        inscripcion.setDisciplina(disciplina);
        inscripcion.setBonificacion(bonif);
        inscripcion.setCostoParticular(costoFinal);

        Inscripcion guardada = inscripcionRepositorio.save(inscripcion);

        return inscripcionMapper.toDTO(guardada);  // 🔹 Asegurarse de que `toDTO` existe en el mapper
    }


    @Override
    public InscripcionResponse obtenerPorId(Long id) {
        Inscripcion ins = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada."));
        return inscripcionMapper.toDTO(ins);
    }

    @Override
    public List<InscripcionResponse> listarInscripciones() {
        return inscripcionRepositorio.findAll().stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InscripcionResponse actualizarInscripcion(Long id, InscripcionRequest request) {
        log.info("Actualizando inscripcion con id: {}", id);
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada."));
        if (!inscripcion.getAlumno().getId().equals(request.alumnoId())) {
            Alumno nuevoAlumno = alumnoRepositorio.findById(request.alumnoId())
                    .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
            inscripcion.setAlumno(nuevoAlumno);
        }
        if (!inscripcion.getDisciplina().getId().equals(request.disciplinaId())) {
            Disciplina nuevaDisciplina = disciplinaRepositorio.findById(request.disciplinaId())
                    .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
            inscripcion.setDisciplina(nuevaDisciplina);
        }
        Bonificacion bonif = null;
        Double costoFinal = request.costoParticular();
        if (request.bonificacionId() != null) {
            bonif = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));
            if (bonif.getPorcentajeDescuento() != null && bonif.getPorcentajeDescuento() > 0) {
                costoFinal = costoFinal - (costoFinal * bonif.getPorcentajeDescuento() / 100);
            }
        }
        inscripcion.setBonificacion(bonif);
        inscripcion.setCostoParticular(costoFinal);
        inscripcion.setNotas(request.notas());
        Inscripcion actualizada = inscripcionRepositorio.save(inscripcion);
        return inscripcionMapper.toDTO(actualizada);
    }

    @Override
    @Transactional
    public void eliminarInscripcion(Long id) {
        Inscripcion ins = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada."));
        inscripcionRepositorio.delete(ins);
    }

    @Override
    public List<InscripcionResponse> listarPorAlumno(Long alumnoId) {
        List<Inscripcion> inscripciones = inscripcionRepositorio.findAllByAlumnoId(alumnoId);
        if (inscripciones == null || inscripciones.isEmpty()) {
            throw new IllegalArgumentException("No hay inscripciones para este alumno.");
        }
        return inscripciones.stream().map(inscripcionMapper::toDTO).collect(Collectors.toList());
    }
}
