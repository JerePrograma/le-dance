package ledance.dto.reporte;

public interface ReportGenerator {
    /**
     * Genera el reporte basandose en los criterios recibidos.
     * @param criteria los filtros y parametros del reporte.
     * @return Un objeto que contiene la informacion del reporte.
     */
    Object generateReport(ReportCriteria criteria);
}
