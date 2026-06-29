package ledance.servicios.pago;

import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Alumno;
import ledance.entidades.EstadoPago;
import ledance.entidades.Inscripcion;
import ledance.entidades.MetodoPago;
import ledance.entidades.Pago;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.BonificacionRepositorio;
import ledance.repositorios.ConceptoRepositorio;
import ledance.repositorios.MetodoPagoRepositorio;
import ledance.repositorios.PagoRepositorio;
import ledance.repositorios.RecargoRepositorio;
import ledance.repositorios.SubConceptoRepositorio;
import ledance.repositorios.UsuarioRepositorio;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.pdfs.ReciboStorageService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagoServicioTest {

    private final AlumnoRepositorio alumnoRepositorio = mock(AlumnoRepositorio.class);
    private final PagoRepositorio pagoRepositorio = mock(PagoRepositorio.class);
    private final PagoMapper pagoMapper = mock(PagoMapper.class);
    private final PaymentProcessor paymentProcessor = mock(PaymentProcessor.class);
    private final PagoServicio service = new PagoServicio(
            alumnoRepositorio,
            pagoRepositorio,
            mock(MetodoPagoRepositorio.class),
            pagoMapper,
            mock(DetallePagoMapper.class),
            mock(RecargoRepositorio.class),
            mock(BonificacionRepositorio.class),
            mock(DetallePagoServicio.class),
            paymentProcessor,
            mock(SubConceptoRepositorio.class),
            mock(ConceptoRepositorio.class),
            mock(ReciboStorageService.class),
            mock(UsuarioRepositorio.class)
    );

    @Test
    void listarVencidosConsultaSoloPagosActivos() {
        when(pagoRepositorio.findPagosVencidos(any(), any())).thenReturn(List.of());

        service.listarPagosVencidos();

        verify(pagoRepositorio).findPagosVencidos(any(LocalDate.class), eq(EstadoPago.ACTIVO));
    }

    @Test
    void registrarPagoNoMutaInscripcionesParaMapearLaRespuesta() {
        Alumno alumno = new Alumno();
        alumno.setId(7L);
        alumno.setInscripciones(new ArrayList<>(List.of(new Inscripcion())));
        MetodoPago metodoPago = new MetodoPago();
        metodoPago.setDescripcion("DEBITO");
        Pago pago = new Pago();
        pago.setId(11L);
        pago.setAlumno(alumno);
        pago.setMetodoPago(metodoPago);
        pago.setMonto(0.0);
        pago.setSaldoRestante(0.0);
        when(alumnoRepositorio.findById(7L)).thenReturn(Optional.of(alumno));
        when(paymentProcessor.obtenerUltimoPagoPendienteEntidad(7L)).thenReturn(pago);

        service.registrarPago(request(7L));

        assertEquals(1, alumno.getInscripciones().size());
        verify(pagoMapper).toDTO(pago);
    }

    private PagoRegistroRequest request(Long alumnoId) {
        AlumnoRegistroRequest alumno = new AlumnoRegistroRequest(
                alumnoId, "Ana", "Perez", null, null, null, null, null,
                null, null, null, null, null, null, true, null, null, List.of());
        return new PagoRegistroRequest(
                null, LocalDate.of(2026, 6, 28), LocalDate.of(2026, 6, 28),
                0.0, 0.0, 0.0, null, alumno, null, List.of(), true, null,
                EstadoPago.ACTIVO.name(), false);
    }
}
