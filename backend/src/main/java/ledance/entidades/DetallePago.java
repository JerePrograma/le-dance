package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "detalle_pagos")
public class DetallePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código único del concepto (opcional)
    private String codigoConcepto;

    @NotNull
    private String concepto;

    // Ejemplo: "01/2025"
    private String cuota;

    @NotNull
    private Double valorBase;

    // Bonificación aplicada (monto de descuento)
    private Double bonificacion = 0.0;

    // Recargo aplicado (monto adicional)
    private Double recargo = 0.0;

    // Saldo a favor (si se aplica crédito previo)
    private Double aFavor = 0.0;

    // Importe calculado: valorBase - bonificacion + recargo - aFavor
    private Double importe;

    // Monto pendiente a cobrar (para pagos parciales)
    private Double aCobrar;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    /**
     * Calcula el importe y asigna aCobrar igual a importe, redondeado a dos decimales.
     */
    public void calcularImporte() {
        double base = (valorBase != null ? valorBase : 0.0);
        double desc = (bonificacion != null ? bonificacion : 0.0);
        double rec = (recargo != null ? recargo : 0.0);
        double favor = (aFavor != null ? aFavor : 0.0);
        double calculado = base - desc + rec - favor;
        // Redondear a 2 decimales
        BigDecimal bd = new BigDecimal(calculado).setScale(2, RoundingMode.HALF_UP);
        this.importe = bd.doubleValue();
        this.aCobrar = this.importe;
    }
}
