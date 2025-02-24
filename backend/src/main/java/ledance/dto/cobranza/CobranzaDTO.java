package ledance.dto.cobranza;

import java.util.List;

public class CobranzaDTO {
    private Long alumnoId;
    private String alumnoNombre;
    private Double totalPendiente;
    private List<DetalleCobranzaDTO> detalles;

    public CobranzaDTO() {
    }

    public CobranzaDTO(Long alumnoId, String alumnoNombre, Double totalPendiente, List<DetalleCobranzaDTO> detalles) {
        this.alumnoId = alumnoId;
        this.alumnoNombre = alumnoNombre;
        this.totalPendiente = totalPendiente;
        this.detalles = detalles;
    }

    public Long getAlumnoId() {
        return alumnoId;
    }

    public void setAlumnoId(Long alumnoId) {
        this.alumnoId = alumnoId;
    }

    public String getAlumnoNombre() {
        return alumnoNombre;
    }

    public void setAlumnoNombre(String alumnoNombre) {
        this.alumnoNombre = alumnoNombre;
    }

    public Double getTotalPendiente() {
        return totalPendiente;
    }

    public void setTotalPendiente(Double totalPendiente) {
        this.totalPendiente = totalPendiente;
    }

    public List<DetalleCobranzaDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCobranzaDTO> detalles) {
        this.detalles = detalles;
    }
}
