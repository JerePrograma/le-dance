package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Representa un Pago realizado por un Alumno.
 * <p>
 * Las relaciones entre los campos son las siguientes:
 * - importeInicial: monto a cobrar originalmente.
 * - montoPagado: suma de los abonos aplicados en este pago.
 * - saldoRestante: lo que aun falta por abonar; debe cumplirse que:
 * montoPagado + saldoRestante = importeInicial.
 * <p>
 * El estado del pago (estadoPago) se actualiza a HISTORICO cuando el saldoRestante es 0.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    private LocalDate fechaVencimiento;

    /**
     * Monto total a cobrar o total abonado (segun la implementacion).
     */
    @NotNull
    private Double monto;

    /**
     * Valor base asociado al pago, en caso de que aplique alguna formula o calculo adicional.
     */
    private Double valorBase;

    @NotNull
    @Min(value = 0, message = "El monto base no puede ser negativo")
    private Double importeInicial;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "metodo_pago_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MetodoPago metodoPago;

    /**
     * Saldo restante por abonar en este pago.
     */
    @NotNull
    private Double saldoRestante = 0.0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    private EstadoPago estadoPago = EstadoPago.ACTIVO;

    private String observaciones;

    @OneToMany(mappedBy = "pago", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH}, orphanRemoval = true)
    @JsonIgnore
    private List<DetallePago> detallePagos = new ArrayList<>();

    /**
     * Monto efectivamente abonado en este pago.
     */
    @NotNull
    @Column(name = "monto_pagado", nullable = false)
    @Min(value = 0, message = "El monto pagado no puede ser negativo")
    private Double montoPagado = 0.0;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;

    @Column(name = "recargo_aplicado")
    private Boolean recargoMetodoPagoAplicado = false;

    @PrePersist
    public void prePersist() {
        if (this.saldoRestante == null) {
            this.saldoRestante = 0.0;
        }
    }

    // Supongamos que tienes un metodo en Pago para remover un detalle:
    public void removerDetalle(DetallePago detalle) {
        this.detallePagos.remove(detalle);
        detalle.setPago(null);
    }

}
