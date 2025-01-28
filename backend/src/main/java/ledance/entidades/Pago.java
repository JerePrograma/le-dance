package ledance.entidades;

import jakarta.persistence.*;
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
    private Double monto;

    @ManyToOne
    private MetodoPago metodoPago; // EFECTIVO, TRANSFERENCIA

    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

    private Boolean recargoAplicado;

    private Boolean bonificacionAplicada;

    @Column(nullable = false)
    private Boolean activo = true;
}
