package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

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

    @NotNull
    @Min(value = 0, message = "El monto debe ser mayor o igual a 0")
    private Double monto;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = true)
    private Inscripcion inscripcion;

    @ManyToOne
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    // Flag que indica si se aplico el recargo
    @Column(nullable = false)
    private Boolean recargoAplicado = false;

    // Flag que indica si se aplico la bonificacion global
    @Column(nullable = false)
    private Boolean bonificacionAplicada = false;

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

    @PrePersist
    @PreUpdate
    public void actualizarImportes() {
        if (detallePagos != null) {
            for (DetallePago detalle : detallePagos) {
                detalle.calcularImporte();
            }
        }
    }

    /**
     * Recalcula el saldo restante basandose en la suma de los abonos ingresados en los detalles.
     * Se asume que cada detalle ya tiene calculado su importe y el abono (ingresado por el usuario).
     */
    public void recalcularSaldoRestante() {
        double totalAbonosDetalles = 0.0;
        if (detallePagos != null) {
            // Aseguramos que cada detalle tenga sus importes actualizados
            detallePagos.forEach(DetallePago::calcularImporte);
            totalAbonosDetalles = detallePagos.stream()
                    .mapToDouble(detalle -> detalle.getAbono() != null ? detalle.getAbono() : 0.0)
                    .sum();
        }
        double nuevoSaldo = this.monto - totalAbonosDetalles;
        this.saldoRestante = nuevoSaldo < 0 ? 0 : nuevoSaldo;
    }

    public String getEstado() {
        return (activo != null && activo) ? "ACTIVO" : "ANULADO";
    }
}
