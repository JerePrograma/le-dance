package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"detallePagos", "pagoMedios", "inscripcion"})
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private LocalDate fechaVencimiento;

    // Monto total calculado inicialmente (suma de importeInicial de los detalles)
    @NotNull
    @Min(value = 0, message = "El monto debe ser mayor o igual a 0")
    private Double monto;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    @JsonIgnoreProperties({"pagos", "inscripciones"})
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = true)
    private Inscripcion inscripcion;

    @ManyToOne
    @JoinColumn(name = "metodo_pago_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private Boolean recargoAplicado = false;

    @Column(nullable = false)
    private Boolean bonificacionAplicada = false;

    // Suma de los importes pendientes de los detalles (monto aún por pagar)
    @NotNull
    private Double saldoRestante;

    @NotNull
    @Column(name = "saldo_a_favor", nullable = false)
    @Min(value = 0, message = "El saldo a favor no puede ser negativo")
    private Double saldoAFavor = 0.0;

    @Column(nullable = false)
    private Boolean activo = true;

    private String observaciones;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DetallePago> detallePagos;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PagoMedio> pagoMedios;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false)
    private TipoPago tipoPago = TipoPago.SUBSCRIPTION;

    // Monto total abonado a lo largo del tiempo
    @NotNull
    @Column(name = "monto_pagado", nullable = false)
    @Min(value = 0, message = "El monto pagado no puede ser negativo")
    private Double montoPagado = 0.0;

    public String getEstado() {
        return (activo != null && activo) ? "ACTIVO" : "ANULADO";
    }
}
