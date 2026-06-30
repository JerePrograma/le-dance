package ledance.servicios.matricula;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Cargo;
import ledance.entidades.EstadoAplicacionPago;
import ledance.entidades.EstadoCargo;
import ledance.entidades.EstadoInscripcion;
import ledance.entidades.EstadoOrigenCargo;
import ledance.entidades.Inscripcion;
import ledance.entidades.Matricula;
import ledance.infra.errores.TratadorDeErrores.OperacionNoPermitidaException;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.AplicacionPagoRepositorio;
import ledance.repositorios.CargoRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MatriculaRepositorio;
import ledance.servicios.cargo.CargoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class MatriculaServicio {
    private static final Logger log = LoggerFactory.getLogger(MatriculaServicio.class);
    private final MatriculaRepositorio matriculas;
    private final AlumnoRepositorio alumnos;
    private final InscripcionRepositorio inscripciones;
    private final CargoRepositorio cargos;
    private final AplicacionPagoRepositorio aplicaciones;
    private final CargoServicio cargoServicio;
    private final Clock clock;

    public MatriculaServicio(MatriculaRepositorio matriculas,
                             AlumnoRepositorio alumnos,
                             InscripcionRepositorio inscripciones,
                             CargoRepositorio cargos,
                             AplicacionPagoRepositorio aplicaciones,
                             CargoServicio cargoServicio,
                             Clock clock) {
        this.matriculas = matriculas;
        this.alumnos = alumnos;
        this.inscripciones = inscripciones;
        this.cargos = cargos;
        this.aplicaciones = aplicaciones;
        this.cargoServicio = cargoServicio;
        this.clock = clock;
    }

    @Transactional
    public MatriculaResponse obtenerOMarcarPendienteMatricula(Long alumnoId, int anio) {
        Alumno alumno = alumnos.findActivoByIdForUpdate(alumnoId)
                .orElseThrow(() -> new OperacionNoPermitidaException("El alumno no existe o está inactivo"));
        Matricula matricula = matriculas.findByAlumnoIdAndAnio(alumnoId, anio).orElseGet(() -> {
            Matricula nueva = new Matricula();
            nueva.setAlumno(alumno);
            nueva.setAnio(anio);
            nueva.setFechaEmision(LocalDate.now(clock));
            nueva.setEstado(EstadoOrigenCargo.EMITIDA);
            matriculas.save(nueva);
            BigDecimal importe = inscripciones.findAllByAlumno_IdAndEstado(alumnoId, EstadoInscripcion.ACTIVA).stream()
                    .map(Inscripcion::getDisciplina)
                    .map(d -> d.getMatricula() == null ? BigDecimal.ZERO : d.getMatricula())
                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.UNNECESSARY);
            cargoServicio.crearParaMatricula(nueva, importe, LocalDate.of(anio, 1, 31));
            return nueva;
        });
        return respuesta(matricula);
    }

    @Transactional
    public MatriculaResponse anular(Long matriculaId) {
        Matricula matricula = matriculas.findById(matriculaId)
                .orElseThrow(() -> new EntityNotFoundException("Matrícula no encontrada"));
        Cargo cargo = cargos.findByMatriculaId(matriculaId)
                .orElseThrow(() -> new IllegalStateException("Matrícula sin cargo"));
        if (aplicaciones.sumByCargoAndEstado(cargo.getId(), EstadoAplicacionPago.APLICADA).signum() > 0) {
            throw new OperacionNoPermitidaException("No puede anularse una matrícula con pagos aplicados");
        }
        matricula.setEstado(EstadoOrigenCargo.ANULADA);
        cargo.setEstado(EstadoCargo.ANULADO);
        return respuesta(matricula);
    }

    @Transactional
    public void generarMatriculasAnioVigente() {
        int anio = Year.now(clock).getValue();
        LinkedHashSet<Long> alumnosActivos = new LinkedHashSet<>();
        inscripciones.findByEstado(EstadoInscripcion.ACTIVA)
                .forEach(i -> alumnosActivos.add(i.getAlumno().getId()));
        alumnosActivos.forEach(id -> obtenerOMarcarPendienteMatricula(id, anio));
        log.info("Matrículas generadas año={} alumnos={}", anio, alumnosActivos.size());
    }

    @Transactional(readOnly = true)
    public MatriculaResponse obtener(Long alumnoId, int anio) {
        return respuesta(matriculas.findByAlumnoIdAndAnio(alumnoId, anio)
                .orElseThrow(() -> new EntityNotFoundException("Matrícula no encontrada")));
    }

    private MatriculaResponse respuesta(Matricula matricula) {
        return new MatriculaResponse(matricula.getId(), matricula.getAnio(), matricula.getFechaEmision(),
                matricula.getEstado().name(), matricula.getAlumno().getId());
    }
}
