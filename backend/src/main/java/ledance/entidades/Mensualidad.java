package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mensualidades")
public class Mensualidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fechaCuota; // Fecha del vencimiento de la mensualidad

    @NotNull
    @Min(value = 0, message = "El valor base debe ser mayor o igual a 0")
    private Double valorBase;

    @ManyToOne
    @JoinColumn(name = "recargo_id", nullable = true) // ✅ Puede ser null si no se aplica
    private Recargo recargo;

    @ManyToOne
    @JoinColumn(name = "bonificacion_id", nullable = true) // ✅ Puede ser null si no se aplica
    private Bonificacion bonificacion;

    @NotNull
    private Double totalPagar; // ✅ Calculado dinámicamente antes de guardar

    @NotNull
    @Enumerated(EnumType.STRING)
    private EstadoMensualidad estado;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    @PrePersist
    @PreUpdate
    public void calcularTotal() {
        double descuento = 0.0;
        double recargoValor = 0.0;

        // Si hay bonificación, aplicarla
        if (bonificacion != null) {
            double descuentoFijo = (bonificacion.getValorFijo() != null ? bonificacion.getValorFijo() : 0.0);
            double descuentoPorcentaje = (bonificacion.getPorcentajeDescuento() != null ?
                    (bonificacion.getPorcentajeDescuento() / 100.0 * valorBase) : 0.0);
            descuento = descuentoFijo + descuentoPorcentaje;
        }

        // Si hay recargo, aplicarlo
        if (recargo != null) {
            double recargoFijo = (recargo.getValorFijo() != null ? recargo.getValorFijo() : 0.0);
            double recargoPorcentaje = (recargo.getPorcentaje() != null ?
                    (recargo.getPorcentaje() / 100.0 * valorBase) : 0.0);
            recargoValor = recargoFijo + recargoPorcentaje;
        }

        this.totalPagar = (valorBase + recargoValor) - descuento;
    }
}
