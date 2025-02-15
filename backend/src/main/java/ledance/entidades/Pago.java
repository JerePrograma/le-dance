package ledance.entidades;

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
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private LocalDate fechaVencimiento;

    // Total del pago calculado como suma de importes de los conceptos (DetallePago)
    @NotNull
    @Min(value = 0, message = "El monto debe ser mayor o igual a 0")
    private Double monto;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    // Relación con Método de Pago (puede ser nulo si se usa más de uno)
    @ManyToOne
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    // Indicador para aplicar recargos (la lógica de cálculo usará este flag)
    @Column(nullable = false)
    private Boolean recargoAplicado = false;

    // Monto de bonificación aplicado (antes era un booleano, ahora es un monto)
    @Column(nullable = false)
    private Double bonificacionAplicada = 0.0;

    // Saldo restante de la transacción (total a pagar menos lo abonado)
    @NotNull
    @Min(value = 0, message = "El saldo restante no puede ser negativo")
    private Double saldoRestante;

    // Saldo a favor que se puede aplicar a este pago
    @NotNull
    @Column(name = "saldo_a_favor", nullable = false)
    @Min(value = 0, message = "El saldo a favor no puede ser negativo")
    private Double saldoAFavor = 0.0;

    @Column(nullable = false)
    private Boolean activo = true;

    // Observaciones (por ejemplo, "deuda mes de enero")
    private String observaciones;

    // Relación con los conceptos que componen el pago
    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePago> detallePagos;

    // Relación con pagos parciales o métodos múltiples
    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoMedio> pagoMedios;
}
