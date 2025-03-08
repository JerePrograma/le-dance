package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @JsonIgnoreProperties({"pagos", "inscripciones"})
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = true)
    private Inscripcion inscripcion;

    @ManyToOne
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    // Flag que indica si se aplicó el recargo
    @Column(nullable = false)
    private Boolean recargoAplicado = false;

    // Flag que indica si se aplicó la bonificación global
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

    // Este método se ejecutará tanto antes de la persistencia como de la actualización
    @PrePersist
    @PreUpdate
    public void actualizarImportes() {
        if (detallePagos != null) {
            detallePagos.forEach(DetallePago::calcularImporte);
        }
        // Recalcular el saldoRestante basado en los importes actualizados de los detalles
        recalcularSaldoRestante();
    }

    /**
     * Recalcula el saldo restante basado en la suma de los importes de cada detalle.
     * Si no hay detalles, se asume que el saldo restante es igual al monto total.
     */
    public void recalcularSaldoRestante() {
        if (detallePagos != null && !detallePagos.isEmpty()) {
            // Actualiza cada detalle y suma sus importes
            detallePagos.forEach(DetallePago::calcularImporte);
            double totalImporte = detallePagos.stream()
                    .mapToDouble(det -> det.getImporte() != null ? det.getImporte() : 0.0)
                    .sum();
            this.saldoRestante = totalImporte;
        } else {
            this.saldoRestante = this.monto;
        }
    }

    public String getEstado() {
        return (activo != null && activo) ? "ACTIVO" : "ANULADO";
    }
}
