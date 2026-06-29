package ledance.servicios.alumno;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.pago.DetallePagoMapper;
import ledance.entidades.Alumno;
import ledance.entidades.AsistenciaAlumnoMensual;
import ledance.entidades.Inscripcion;
import ledance.entidades.Matricula;
import ledance.entidades.Mensualidad;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.DetallePagoRepositorio;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlumnoServicioTest {

    private static final LocalDate FECHA_NEGOCIO = LocalDate.of(2026, 6, 29);
    private final AlumnoRepositorio alumnoRepositorio = mock(AlumnoRepositorio.class);
    private final AlumnoServicio service = new AlumnoServicio(
            alumnoRepositorio,
            mock(AlumnoMapper.class),
            mock(DisciplinaMapper.class),
            mock(DetallePagoRepositorio.class),
            mock(DetallePagoMapper.class),
            Clock.fixed(
                    Instant.parse("2026-06-29T15:00:00Z"),
                    ZoneId.of("America/Argentina/Buenos_Aires"))
    );

    @Test
    void darBajaEsIdempotenteYConservaHistorialAsociado() {
        AsistenciaAlumnoMensual asistencia = new AsistenciaAlumnoMensual();
        Mensualidad mensualidad = new Mensualidad();
        Inscripcion inscripcion = new Inscripcion();
        List<AsistenciaAlumnoMensual> asistencias = new ArrayList<>(List.of(asistencia));
        List<Mensualidad> mensualidades = new ArrayList<>(List.of(mensualidad));
        inscripcion.setAsistenciasAlumnoMensual(asistencias);
        inscripcion.setMensualidades(mensualidades);
        Matricula matricula = new Matricula();
        List<Inscripcion> inscripciones = new ArrayList<>(List.of(inscripcion));
        List<Matricula> matriculas = new ArrayList<>(List.of(matricula));
        Alumno alumno = new Alumno();
        alumno.setId(7L);
        alumno.setActivo(true);
        alumno.setInscripciones(inscripciones);
        alumno.setMatriculas(matriculas);
        when(alumnoRepositorio.findById(7L)).thenReturn(Optional.of(alumno));

        service.darBajaAlumno(7L);
        service.darBajaAlumno(7L);

        assertAll(
                () -> assertFalse(alumno.getActivo()),
                () -> assertEquals(FECHA_NEGOCIO, alumno.getFechaDeBaja()),
                () -> assertSame(inscripciones, alumno.getInscripciones()),
                () -> assertSame(inscripcion, alumno.getInscripciones().getFirst()),
                () -> assertSame(asistencias, inscripcion.getAsistenciasAlumnoMensual()),
                () -> assertSame(asistencia, inscripcion.getAsistenciasAlumnoMensual().getFirst()),
                () -> assertSame(mensualidades, inscripcion.getMensualidades()),
                () -> assertSame(mensualidad, inscripcion.getMensualidades().getFirst()),
                () -> assertSame(matriculas, alumno.getMatriculas()),
                () -> assertSame(matricula, alumno.getMatriculas().getFirst())
        );
        verify(alumnoRepositorio, times(2)).findById(7L);
        verify(alumnoRepositorio).save(alumno);
    }
}
