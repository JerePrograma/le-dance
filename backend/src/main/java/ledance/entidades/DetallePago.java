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

    // Para auditoría: monto que se abono a favor (por ejemplo, créditos o saldos a favor)
    private Double aFavor = 0.0;

    // Valor base del concepto (precio original sin descuentos ni recargos)
    @NotNull
    private Double valorBase;

    // Monto calculado inicialmente (valorBase - descuento + recargo)
    // Este campo se establece al crear el detalle y no se modifica con abonos, para fines de auditoría
    private Double importeInicial;

    // Monto pendiente por abonar. Se actualizará restando los abonos parciales
    private Double importePendiente;

    // Monto que se aplicará en el proximo abono (puede actualizarse en cada operacion)
    private Double aCobrar;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    // Campo para marcar si el detalle ya fue cobrado (importePendiente==0)
    @Column(nullable = false)
    private Boolean cobrado = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDetallePago tipo = TipoDetallePago.GENERAL;

    public Double getaCobrar() {
        return aCobrar;
    }

    public void setaCobrar(Double aCobrar) {
        this.aCobrar = aCobrar;
    }
}
