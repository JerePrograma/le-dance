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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MensualidadServicioTest {

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
            mock(ProfesorRepositorio.class)
    );

    @Test
    void recalcularConservaImporteInicialYActualizaSaldoPendiente() {
        Mensualidad mensualidad = mensualidad(100.00, 30.00);

        service.recalcularImportePendiente(mensualidad);

        assertEquals(100.00, mensualidad.getImporteInicial());
        assertEquals(70.00, mensualidad.getImportePendiente());
        assertEquals(EstadoMensualidad.PENDIENTE, mensualidad.getEstado());
    }

    @Test
    void recalcularRechazaSobrepagoSinMutarLaMensualidad() {
        Mensualidad mensualidad = mensualidad(100.00, 100.01);

        assertThrows(IllegalArgumentException.class,
                () -> service.recalcularImportePendiente(mensualidad));

        assertEquals(100.00, mensualidad.getImporteInicial());
        assertEquals(100.01, mensualidad.getMontoAbonado());
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
