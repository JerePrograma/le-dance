package ledance.servicios;

import ledance.dto.request.InscripcionRequest;
import ledance.dto.response.InscripcionResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Bonificacion;
import ledance.entidades.Disciplina;
import ledance.entidades.Inscripcion;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.BonificacionRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InscripcionServicio {

    private final InscripcionRepositorio inscripcionRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;

    public InscripcionServicio(InscripcionRepositorio inscripcionRepositorio,
                               AlumnoRepositorio alumnoRepositorio,
                               DisciplinaRepositorio disciplinaRepositorio,
                               BonificacionRepositorio bonificacionRepositorio) {
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
    }

    public InscripcionResponse crearInscripcion(InscripcionRequest request) {
        Alumno alumno = alumnoRepositorio.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        Disciplina disciplina = disciplinaRepositorio.findById(request.disciplinaId())
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));

        Bonificacion bonif = null;
        if (request.bonificacionId() != null) {
            bonif = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));
        }

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setAlumno(alumno);
        inscripcion.setDisciplina(disciplina);
        inscripcion.setBonificacion(bonif);
        inscripcion.setCostoParticular(request.costoParticular());
        inscripcion.setNotas(request.notas());

        // Guardar
        Inscripcion guardada = inscripcionRepositorio.save(inscripcion);

        return toResponse(guardada);
    }

    public InscripcionResponse obtenerPorId(Long id) {
        Inscripcion ins = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
        return toResponse(ins);
    }

    public List<InscripcionResponse> listarInscripciones() {
        return inscripcionRepositorio.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public InscripcionResponse actualizarInscripcion(Long id, InscripcionRequest request) {
        Inscripcion ins = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));

        // Actualizar datos
        if (!ins.getAlumno().getId().equals(request.alumnoId())) {
            // Podrías permitir o no cambiar el alumno.
            Alumno nuevoAlumno = alumnoRepositorio.findById(request.alumnoId())
                    .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
            ins.setAlumno(nuevoAlumno);
        }

        if (!ins.getDisciplina().getId().equals(request.disciplinaId())) {
            // Cambiar disciplina
            Disciplina nuevaDisciplina = disciplinaRepositorio.findById(request.disciplinaId())
                    .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
            ins.setDisciplina(nuevaDisciplina);
        }

        if (request.bonificacionId() != null) {
            Bonificacion nuevaBonif = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada."));
            ins.setBonificacion(nuevaBonif);
        } else {
            ins.setBonificacion(null);
        }

        ins.setCostoParticular(request.costoParticular());
        ins.setNotas(request.notas());

        Inscripcion actualizada = inscripcionRepositorio.save(ins);
        return toResponse(actualizada);
    }

    public void eliminarInscripcion(Long id) {
        Inscripcion ins = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
        inscripcionRepositorio.delete(ins);
    }

    private InscripcionResponse toResponse(Inscripcion ins) {
        return new InscripcionResponse(
                ins.getId(),
                ins.getAlumno().getId(),
                ins.getDisciplina().getId(),
                ins.getBonificacion() != null ? ins.getBonificacion().getId() : null,
                ins.getCostoParticular(),
                ins.getNotas()
        );
    }

    public List<InscripcionResponse> listarPorAlumno(Long alumnoId) {
        return inscripcionRepositorio.findAllByAlumnoId(alumnoId);
    }
}
