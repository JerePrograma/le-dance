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

    private String codigoConcepto;

    @NotNull
    private String concepto;

    private String cuota;

    @NotNull
    private Double valorBase;

    private Double bonificacion = 0.0;

    private Double recargo = 0.0;

    private Double aFavor = 0.0;

    private Double importe;

    // Monto pendiente a cobrar (para pagos parciales)
    private Double aCobrar;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    public void calcularImporte() {
        double base = (valorBase != null ? valorBase : 0.0);
        double desc = (bonificacion != null ? bonificacion : 0.0);
        double rec = (recargo != null ? recargo : 0.0);
        double favor = (aFavor != null ? aFavor : 0.0);
        double calculado = base - desc + rec - favor;
        BigDecimal bd = new BigDecimal(calculado).setScale(2, RoundingMode.HALF_UP);
        this.importe = bd.doubleValue();
        this.aCobrar = this.importe;
    }

    @PrePersist
    @PreUpdate
    public void prePersistUpdate() {
        calcularImporte();
    }

    public String getEstadoDetalle() {
        if (aCobrar == 0) {
            return "SALDADO " + concepto;
        } else if (aCobrar < importe) {
            return "A CUENTA " + concepto;
        } else {
            return "PENDIENTE " + concepto;
        }
    }
}
