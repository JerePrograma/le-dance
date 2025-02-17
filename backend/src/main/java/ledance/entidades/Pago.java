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

    // Total del pago calculado (costo final)
    @NotNull
    @Min(value = 0, message = "El monto debe ser mayor o igual a 0")
    private Double monto;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    // Relación con el método de pago (opcional)
    @ManyToOne
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private Boolean recargoAplicado = false;

    @Column(nullable = false)
    private Double bonificacionAplicada = 0.0;

    @NotNull
    @Min(value = 0, message = "El saldo restante no puede ser negativo")
    private Double saldoRestante;

    @NotNull
    @Column(name = "saldo_a_favor", nullable = false)
    @Min(value = 0, message = "El saldo a favor no puede ser negativo")
    private Double saldoAFavor = 0.0;

    @Column(nullable = false)
    private Boolean activo = true;

    private String observaciones;

    // Relación con los detalles del pago
    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePago> detallePagos;

    // Relación con los medios de pago (para pagos parciales o múltiples)
    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoMedio> pagoMedios;
}
