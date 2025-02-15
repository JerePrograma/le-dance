package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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

    // Nombre del concepto (por ejemplo, "Flex y Contorsión")
    @NotNull
    private String concepto;

    // Ejemplo: "01/2025"
    private String cuota;

    // Valor base del concepto
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

    // Relación con el pago principal
    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    /**
     * Calcula el importe y asigna aCobrar igual a importe.
     */
    public void calcularImporte() {
        double base = (valorBase != null ? valorBase : 0.0);
        double desc = (bonificacion != null ? bonificacion : 0.0);
        double rec = (recargo != null ? recargo : 0.0);
        double aF = (aFavor != null ? aFavor : 0.0);
        this.importe = base - desc + rec - aF;
        this.aCobrar = this.importe;
    }
}
