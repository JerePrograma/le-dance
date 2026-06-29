package ledance.servicios.inscripcion;

import ledance.dto.inscripcion.InscripcionMapper;
import ledance.entidades.AsistenciaAlumnoMensual;
import ledance.entidades.EstadoInscripcion;
import ledance.entidades.Inscripcion;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.AsistenciaAlumnoMensualRepositorio;
import ledance.repositorios.AsistenciaDiariaRepositorio;
import ledance.repositorios.BonificacionRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MensualidadRepositorio;
import ledance.repositorios.PagoRepositorio;
import ledance.servicios.asistencia.AsistenciaDiariaServicio;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.pago.PaymentProcessor;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InscripcionServicioTest {

    private static final LocalDate FECHA_NEGOCIO = LocalDate.of(2026, 6, 29);
    private final InscripcionRepositorio inscripcionRepositorio = mock(InscripcionRepositorio.class);
    private final InscripcionServicio service = new InscripcionServicio(
            inscripcionRepositorio,
            mock(AlumnoRepositorio.class),
            mock(DisciplinaRepositorio.class),
            mock(BonificacionRepositorio.class),
            mock(InscripcionMapper.class),
            mock(AsistenciaMensualServicio.class),
            mock(MensualidadServicio.class),
            mock(AsistenciaAlumnoMensualRepositorio.class),
            mock(MatriculaServicio.class),
            mock(PaymentProcessor.class),
            mock(PagoRepositorio.class),
            mock(AsistenciaDiariaServicio.class),
            mock(MensualidadRepositorio.class),
            mock(AsistenciaDiariaRepositorio.class),
            Clock.fixed(
                    Instant.parse("2026-06-29T15:00:00Z"),
                    ZoneId.of("America/Argentina/Buenos_Aires"))
    );

    @Test
    void darBajaInscripcionConservaAsistencias() {
        AsistenciaAlumnoMensual asistencia = new AsistenciaAlumnoMensual();
        List<AsistenciaAlumnoMensual> asistencias = new ArrayList<>(List.of(asistencia));
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(9L);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setAsistenciasAlumnoMensual(asistencias);
        when(inscripcionRepositorio.findById(9L)).thenReturn(Optional.of(inscripcion));

        service.eliminarInscripcion(9L);

        assertAll(
                () -> assertEquals(EstadoInscripcion.INACTIVA, inscripcion.getEstado()),
                () -> assertEquals(FECHA_NEGOCIO, inscripcion.getFechaBaja()),
                () -> assertSame(asistencias, inscripcion.getAsistenciasAlumnoMensual()),
                () -> assertSame(asistencia, inscripcion.getAsistenciasAlumnoMensual().getFirst())
        );
        verify(inscripcionRepositorio).save(inscripcion);
    }
}
