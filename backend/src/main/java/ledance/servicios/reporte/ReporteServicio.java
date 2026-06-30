package ledance.servicios.reporte;

import ledance.dto.reporte.request.ReporteLiquidacionRequest;
import ledance.dto.reporte.response.ReporteMensualidadResponse;
import ledance.entidades.Cargo;
import ledance.entidades.EstadoAplicacionPago;
import ledance.entidades.TipoCargo;
import ledance.repositorios.AplicacionPagoRepositorio;
import ledance.repositorios.CargoRepositorio;
import ledance.servicios.pdfs.PdfService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReporteServicio {
    private final CargoRepositorio cargos;
    private final AplicacionPagoRepositorio aplicaciones;
    private final PdfService pdf;

    public ReporteServicio(CargoRepositorio cargos, AplicacionPagoRepositorio aplicaciones, PdfService pdf) {
        this.cargos = cargos;
        this.aplicaciones = aplicaciones;
        this.pdf = pdf;
    }

    @Transactional(readOnly = true)
    public List<ReporteMensualidadResponse> buscar(LocalDate desde, LocalDate hasta,
                                                   Long disciplinaId, Long profesorId) {
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        return cargos.findMensualidadesParaReporte(TipoCargo.MENSUALIDAD, desde, hasta, disciplinaId, profesorId)
                .stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportar(ReporteLiquidacionRequest request) {
        BigDecimal porcentaje = request.porcentajeEscuela().setScale(4, RoundingMode.UNNECESSARY);
        if (porcentaje.signum() < 0 || porcentaje.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        return pdf.generarLiquidacionProfesorPdf(buscar(request.fechaInicio(), request.fechaFin(),
                request.disciplinaId(), request.profesorId()), request.fechaInicio(), request.fechaFin(), porcentaje);
    }

    private ReporteMensualidadResponse respuesta(Cargo cargo) {
        BigDecimal cobrado = aplicaciones.sumByCargoAndEstado(cargo.getId(), EstadoAplicacionPago.APLICADA);
        var disciplina = cargo.getMensualidad().getInscripcion().getDisciplina();
        return new ReporteMensualidadResponse(cargo.getId(), cargo.getFechaEmision(),
                (cargo.getAlumno().getApellido() + " " + cargo.getAlumno().getNombre()).trim(),
                disciplina.getNombre(),
                (disciplina.getProfesor().getApellido() + " " + disciplina.getProfesor().getNombre()).trim(),
                decimal(cargo.getImporteOriginal()), decimal(cobrado),
                decimal(cargo.getImporteOriginal().subtract(cobrado)), cargo.getEstado().name());
    }

    private static String decimal(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
