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

    // Código único del concepto
    private String codigoConcepto;

    // Nombre del concepto (p. ej., "Flex y Contorsión")
    @NotNull
    private String concepto;

    // Por ejemplo: "01/2025"
    private String cuota;

    // Valor base del concepto
    @NotNull
    private Double valorBase;

    // Descuento aplicado
    private Double bonificacion = 0.0;

    // Recargo aplicado
    private Double recargo = 0.0;

    // Saldo a favor (si se descuenta un crédito previo)
    private Double aFavor = 0.0;

    // Importe calculado: valorBase - bonificacion + recargo - aFavor
    private Double importe;

    // Monto pendiente a cobrar (en caso de pago parcial)
    private Double aCobrar;

    // Relación con el pago principal
    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    // Método auxiliar para calcular el importe (puede llamarse antes de persistir)
    public void calcularImporte() {
        double base = valorBase != null ? valorBase : 0.0;
        double desc = bonificacion != null ? bonificacion : 0.0;
        double rec = recargo != null ? recargo : 0.0;
        double aF = aFavor != null ? aFavor : 0.0;
        this.importe = base - desc + rec - aF;
        // Por defecto, aCobrar es igual al importe inicial
        this.aCobrar = this.importe;
    }
}
