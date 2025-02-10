package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private LocalDate fechaVencimiento; // ✅ Nueva columna

    @NotNull
    @Min(value = 0, message = "El monto debe ser mayor o igual a 0")
    private Double monto;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false) // ✅ Relacionado con la inscripción
    private Inscripcion inscripcion;

    @ManyToOne
    @JoinColumn(name = "metodo_pago_id") // ✅ Relación con método de pago
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private Boolean recargoAplicado = false;

    @Column(nullable = false)
    private Boolean bonificacionAplicada = false;

    @NotNull
    @Min(value = 0, message = "El saldo restante no puede ser negativo")
    private Double saldoRestante; // ✅ Manejo de pagos parciales

    @Column(nullable = false)
    private Boolean activo = true;
}
