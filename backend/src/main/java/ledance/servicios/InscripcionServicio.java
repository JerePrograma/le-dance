package ledance.servicios;

import ledance.dto.request.InscripcionRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.BonificacionResponse;
import ledance.dto.response.DisciplinaSimpleResponse;
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
        Double costoFinal = request.costoParticular(); // Costo base

        if (request.bonificacionId() != null) {
            bonif = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificación no encontrada."));

            // Aplicar el descuento de la bonificación
            if (bonif.getPorcentajeDescuento() != null && bonif.getPorcentajeDescuento() > 0) {
                costoFinal = costoFinal - (costoFinal * bonif.getPorcentajeDescuento() / 100);
            }
        }

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setAlumno(alumno);
        inscripcion.setDisciplina(disciplina);
        inscripcion.setBonificacion(bonif);
        inscripcion.setCostoParticular(costoFinal);
        inscripcion.setNotas(request.notas());

        // Guardar inscripción
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
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));

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
        Double costoFinal = request.costoParticular(); // Costo base

        if (request.bonificacionId() != null) {
            bonif = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificación no encontrada."));

            // Aplicar el descuento de la bonificación
            if (bonif.getPorcentajeDescuento() != null && bonif.getPorcentajeDescuento() > 0) {
                costoFinal = costoFinal - (costoFinal * bonif.getPorcentajeDescuento() / 100);
            }
        }

        inscripcion.setBonificacion(bonif);
        inscripcion.setCostoParticular(costoFinal);
        inscripcion.setNotas(request.notas());

        Inscripcion actualizada = inscripcionRepositorio.save(inscripcion);
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
                new AlumnoListadoResponse(ins.getAlumno().getId(), ins.getAlumno().getNombre(), ins.getAlumno().getApellido()),
                new DisciplinaSimpleResponse(ins.getDisciplina().getId(), ins.getDisciplina().getNombre()),
                ins.getBonificacion() != null
                        ? new BonificacionResponse(
                        ins.getBonificacion().getId(),
                        ins.getBonificacion().getDescripcion(),
                        ins.getBonificacion().getPorcentajeDescuento(),
                        ins.getBonificacion().getActivo(),
                        ins.getBonificacion().getObservaciones())
                        : null,
                ins.getCostoParticular(),
                ins.getNotas()
        );
    }


    public List<InscripcionResponse> listarPorAlumno(Long alumnoId) {
        return inscripcionRepositorio.findAllByAlumnoId(alumnoId);
    }
}
