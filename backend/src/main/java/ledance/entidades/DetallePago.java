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

    private String codigoConcepto;

    @NotNull
    private String concepto;

    private String cuota;

    @ManyToOne
    @JoinColumn(name = "bonificacion_id")
    private Bonificacion bonificacion;

    @ManyToOne
    @JoinColumn(name = "recargo_id")
    private Recargo recargo;

    // Inicializado en 0.0 si no se envía
    private Double aFavor = 0.0;

    @NotNull
    private Double valorBase;

    private Double importe;

    private Double aCobrar;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    // Nuevo campo para marcar si el detalle ya fue cobrado
    @Column(nullable = false)
    private Boolean cobrado = false;

    public Double getaCobrar() {
        return aCobrar;
    }

    public void setaCobrar(Double aCobrar) {
        this.aCobrar = aCobrar;
    }
}
