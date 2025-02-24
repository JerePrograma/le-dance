package ledance.dto.cobranza;

public class DetalleCobranzaDTO {
    private String concepto;
    private Double pendiente;

    public DetalleCobranzaDTO() {
    }

    public DetalleCobranzaDTO(String concepto, Double pendiente) {
        this.concepto = concepto;
        this.pendiente = pendiente;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public Double getPendiente() {
        return pendiente;
    }

    public void setPendiente(Double pendiente) {
        this.pendiente = pendiente;
    }
}
