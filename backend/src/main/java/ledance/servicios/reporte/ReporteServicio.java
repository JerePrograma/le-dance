package ledance.servicios.reporte;

import ledance.dto.reporte.ReporteMapper;
import ledance.dto.reporte.request.ReporteLiquidacionRequest;
import ledance.dto.reporte.request.ReporteRegistroRequest;
import ledance.dto.reporte.response.ReporteResponse;
import ledance.entidades.Reporte;
import ledance.entidades.Usuario;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.ReporteRepositorio;
import ledance.repositorios.UsuarioRepositorio;
import jakarta.transaction.Transactional;
import ledance.servicios.pdfs.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteServicio implements IReporteServicio {

    private static final Logger log = LoggerFactory.getLogger(ReporteServicio.class);

    private final ReporteRepositorio reporteRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ReporteMapper reporteMapper;
    private final PdfService pdfService;

    public ReporteServicio(ReporteRepositorio reporteRepositorio,
                           InscripcionRepositorio inscripcionRepositorio,
                           UsuarioRepositorio usuarioRepositorio,
                           ReporteMapper reporteMapper, PdfService pdfService) {
        this.reporteRepositorio = reporteRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.reporteMapper = reporteMapper;
        this.pdfService = pdfService;
    }

    /**
     * Genera un reporte generico a partir de un request.
     */
    @Override
    @Transactional
    public ReporteResponse generarReporte(ReporteRegistroRequest request) {
        log.info("Generando reporte de tipo: {}", request.tipo());
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo(request.tipo());
        nuevoReporte.setDescripcion("Reporte generado de forma generica.");
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (request.usuarioId() != null) {
            Usuario usuario = usuarioRepositorio.findById(request.usuarioId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    /**
     * Genera un reporte de recaudacion por disciplina.
     */
    @Override
    @Transactional
    public ReporteResponse generarReporteRecaudacionPorDisciplina(Long disciplinaId, Long usuarioId) {
        log.info("Generando reporte de recaudacion para la disciplina con ID: {}", disciplinaId);
        List<Object[]> resultados = inscripcionRepositorio.obtenerRecaudacionPorDisciplina(disciplinaId);
        String descripcion = resultados.isEmpty()
                ? "No se encontraron datos de recaudacion para la disciplina."
                : resultados.stream()
                .map(r -> "Disciplina: " + r[0] + " | Monto Total: $" + r[1])
                .collect(Collectors.joining("\n"));

        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Recaudacion por Disciplina");
        nuevoReporte.setDescripcion(descripcion);
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (usuarioId != null) {
            Usuario usuario = usuarioRepositorio.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    /**
     * Genera un reporte de asistencias por alumno.
     * (En un caso real, aqui se consultaria un repositorio de asistencias.)
     */
    @Override
    @Transactional
    public ReporteResponse generarReporteAsistenciasPorAlumno(Long alumnoId, Long usuarioId) {
        log.info("Generando reporte de asistencias para el alumno con ID: {}", alumnoId);
        String descripcion = "Reporte de asistencias para el alumno con ID " + alumnoId;
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Asistencias por Alumno");
        nuevoReporte.setDescripcion(descripcion);
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (usuarioId != null) {
            Usuario usuario = usuarioRepositorio.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    /**
     * Genera un reporte de asistencias por disciplina.
     */
    @Override
    @Transactional
    public ReporteResponse generarReporteAsistenciasPorDisciplina(Long disciplinaId, Long usuarioId) {
        log.info("Generando reporte de asistencias para la disciplina con ID: {}", disciplinaId);
        String descripcion = "Reporte de asistencias para la disciplina con ID " + disciplinaId;
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Asistencias por Disciplina");
        nuevoReporte.setDescripcion(descripcion);
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (usuarioId != null) {
            Usuario usuario = usuarioRepositorio.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    /**
     * Genera un reporte de asistencias por disciplina y alumno.
     */
    @Override
    @Transactional
    public ReporteResponse generarReporteAsistenciasPorDisciplinaAlumno(Long disciplinaId, Long alumnoId, Long usuarioId) {
        log.info("Generando reporte de asistencias para el alumno con ID {} en la disciplina con ID {}", alumnoId, disciplinaId);
        String descripcion = "Reporte de asistencias para el alumno con ID " + alumnoId +
                " en la disciplina con ID " + disciplinaId;
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Asistencias por Disciplina y Alumno");
        nuevoReporte.setDescripcion(descripcion);
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (usuarioId != null) {
            Usuario usuario = usuarioRepositorio.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    @Override
    public List<ReporteResponse> listarReportes() {
        return reporteRepositorio.findAll().stream()
                .map(reporteMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ReporteResponse obtenerReportePorId(Long id) {
        Reporte reporte = reporteRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        return reporteMapper.toDTO(reporte);
    }

    @Override
    @Transactional
    public void eliminarReporte(Long id) {
        Reporte reporte = reporteRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        reporte.setActivo(false);
        reporteRepositorio.save(reporte);
    }

    // Ejemplo de reporte con filtros y paginacion:
    public Page<ReporteResponse> generarReportePaginado(
            Long usuarioId, String tipo, LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable) {
        log.info("Generando reporte paginado para usuario: {}, tipo: {}, entre {} y {}",
                usuarioId, tipo, fechaInicio, fechaFin);

        // Se pueden construir dinamicamente las condiciones (usando Specification, por ejemplo)
        Specification<Reporte> spec = Specification.where(null);
        if (usuarioId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId));
        }
        if (tipo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo));
        }
        if (fechaInicio != null && fechaFin != null) {
            spec = spec.and((root, query, cb) -> cb.between(root.get("fechaGeneracion"), fechaInicio, fechaFin));
        }
        Page<Reporte> reportes = reporteRepositorio.findAll(spec, pageable);
        return reportes.map(reporteMapper::toDTO);
    }

    // en ReporteServicio.java
    public byte[] exportarLiquidacionProfesor(ReporteLiquidacionRequest req) {
        return pdfService.generarLiquidacionProfesorPdf(
                req.profesor(),
                req.disciplina(),
                req.detalles(),
                req.porcentaje()
        );
    }

//    public ByteArrayInputStream exportarReporteAExcel(List<ReporteResponse> reportes) {
//        try (Workbook workbook = new XSSFWorkbook()) {
//            Sheet sheet = workbook.createSheet("Reportes");
//            Row header = sheet.createRow(0);
//            header.createCell(0).setCellValue("ID");
//            header.createCell(1).setCellValue("Tipo");
//            header.createCell(2).setCellValue("Descripcion");
//            header.createCell(3).setCellValue("Fecha Generacion");
//            header.createCell(4).setCellValue("Usuario");
//
//            int rowIdx = 1;
//            for (ReporteResponse rep : reportes) {
//                Row row = sheet.createRow(rowIdx++);
//                row.createCell(0).setCellValue(rep.getId());
//                row.createCell(1).setCellValue(rep.getTipo());
//                row.createCell(2).setCellValue(rep.getDescripcion());
//                row.createCell(3).setCellValue(rep.getFechaGeneracion().toString());
//                row.createCell(4).setCellValue(rep.getUsuarioNombre());
//            }
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            workbook.write(out);
//            return new ByteArrayInputStream(out.toByteArray());
//        } catch (IOException e) {
//            log.error("Error exportando a Excel: {}", e.getMessage());
//            throw new RuntimeException("Error al exportar reporte");
//        }
//    }

}
