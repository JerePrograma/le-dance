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

    // Total del pago calculado como suma de importes de DetallePago
    @NotNull
    @Min(value = 0, message = "El monto debe ser mayor o igual a 0")
    private Double monto;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    // Agregamos la relación con MetodoPago
    @ManyToOne
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private Boolean recargoAplicado = false;

    @Column(nullable = false)
    private Boolean bonificacionAplicada = false;

    // Saldo restante de la transacción (total a pagar menos lo abonado mediante PagoMedio)
    @NotNull
    @Min(value = 0, message = "El saldo restante no puede ser negativo")
    private Double saldoRestante;

    @Column(nullable = false)
    private Boolean activo = true;

    // Observaciones sobre el pago (por ejemplo, "deuda mes de enero")
    private String observaciones;

    // Relación con DetallePago: un pago puede tener múltiples conceptos
    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePago> detallePagos;

    // Relación con PagoMedio: pagos parciales o múltiples métodos
    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoMedio> pagoMedios;
}
