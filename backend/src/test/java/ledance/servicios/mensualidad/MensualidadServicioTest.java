package ledance.servicios.mensualidad;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.mensualidad.MensualidadMapper;
import ledance.entidades.EstadoMensualidad;
import ledance.entidades.Mensualidad;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.BonificacionRepositorio;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MensualidadRepositorio;
import ledance.repositorios.PagoRepositorio;
import ledance.repositorios.ProcesoEjecutadoRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import ledance.repositorios.RecargoRepositorio;
import ledance.servicios.recargo.RecargoServicio;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MensualidadServicioTest {

    private static final LocalDate FECHA_NEGOCIO = LocalDate.of(2026, 6, 15);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-15T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
    private final MensualidadServicio service = new MensualidadServicio(
            mock(DetallePagoRepositorio.class),
            mock(MensualidadRepositorio.class),
            mock(InscripcionRepositorio.class),
            mock(MensualidadMapper.class),
            mock(RecargoRepositorio.class),
            mock(BonificacionRepositorio.class),
            mock(ProcesoEjecutadoRepositorio.class),
            mock(RecargoServicio.class),
            mock(DisciplinaRepositorio.class),
            mock(PagoRepositorio.class),
            mock(AlumnoMapper.class),
            mock(AlumnoRepositorio.class),
            mock(ProfesorRepositorio.class),
            clock
    );

    @Test
    void recalcularPagoParcialConservaImporteInicialActualizaSaldoYLimpiaFechaPago() {
        Mensualidad mensualidad = mensualidad(100.00, 30.00);
        mensualidad.setFechaPago(FECHA_NEGOCIO.minusDays(1));

        service.recalcularImportePendiente(mensualidad);

        assertAll(
                () -> assertEquals(100.00, mensualidad.getImporteInicial()),
                () -> assertEquals(70.00, mensualidad.getImportePendiente()),
                () -> assertEquals(EstadoMensualidad.PENDIENTE, mensualidad.getEstado()),
                () -> assertNull(mensualidad.getFechaPago())
        );
    }

    @Test
    void recalcularPagoTotalUsaFechaDeNegocio() {
        Mensualidad mensualidad = mensualidad(100.00, 100.00);

        service.recalcularImportePendiente(mensualidad);

        assertAll(
                () -> assertEquals(100.00, mensualidad.getImporteInicial()),
                () -> assertEquals(0.00, mensualidad.getImportePendiente()),
                () -> assertEquals(EstadoMensualidad.PAGADO, mensualidad.getEstado()),
                () -> assertEquals(FECHA_NEGOCIO, mensualidad.getFechaPago())
        );
    }

    @Test
    void recalcularRechazaSobrepagoSinMutarLaMensualidad() {
        Mensualidad mensualidad = mensualidad(100.00, 100.01);
        mensualidad.setImportePendiente(25.00);
        mensualidad.setEstado(EstadoMensualidad.PAGADO);
        mensualidad.setFechaPago(FECHA_NEGOCIO.minusDays(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.recalcularImportePendiente(mensualidad));

        assertAll(
                () -> assertEquals(100.00, mensualidad.getImporteInicial()),
                () -> assertEquals(100.01, mensualidad.getMontoAbonado()),
                () -> assertEquals(25.00, mensualidad.getImportePendiente()),
                () -> assertEquals(EstadoMensualidad.PAGADO, mensualidad.getEstado()),
                () -> assertEquals(FECHA_NEGOCIO.minusDays(1), mensualidad.getFechaPago())
        );
    }

    @Test
    void recalcularRechazaMontoAbonadoNegativoSinMutarSaldo() {
        Mensualidad mensualidad = mensualidad(100.00, -0.01);

        assertThrows(IllegalArgumentException.class,
                () -> service.recalcularImportePendiente(mensualidad));

        assertEquals(100.00, mensualidad.getImportePendiente());
    }

    @Test
    void recalcularRechazaImporteInicialNegativoSinMutarSaldo() {
        Mensualidad mensualidad = mensualidad(-0.01, 0.00);

        assertThrows(IllegalArgumentException.class,
                () -> service.recalcularImportePendiente(mensualidad));

        assertEquals(-0.01, mensualidad.getImportePendiente());
    }

    @Test
    void recalcularRepetidoNoDegradaImporteOriginal() {
        Mensualidad mensualidad = mensualidad(100.00, 30.00);

        service.recalcularImportePendiente(mensualidad);
        service.recalcularImportePendiente(mensualidad);

        assertAll(
                () -> assertEquals(100.00, mensualidad.getImporteInicial()),
                () -> assertEquals(70.00, mensualidad.getImportePendiente())
        );
    }

    private Mensualidad mensualidad(double importeInicial, double montoAbonado) {
        Mensualidad mensualidad = new Mensualidad();
        mensualidad.setImporteInicial(importeInicial);
        mensualidad.setMontoAbonado(montoAbonado);
        mensualidad.setImportePendiente(importeInicial);
        mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
        return mensualidad;
    }
}
