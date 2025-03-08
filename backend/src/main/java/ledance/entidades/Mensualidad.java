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
@Table(name = "mensualidades")
public class Mensualidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fechaGeneracion;

    @NotNull
    private LocalDate fechaCuota;

    // Nuevo campo para registrar la fecha en que se realizó el pago (puede ser null si aún no se paga)
    private LocalDate fechaPago;

    @NotNull
    @Min(value = 0, message = "El valor base debe ser mayor o igual a 0")
    private Double valorBase;

    @ManyToOne
    @JoinColumn(name = "recargo_id", nullable = true)
    private Recargo recargo;

    @ManyToOne
    @JoinColumn(name = "bonificacion_id", nullable = true)
    private Bonificacion bonificacion;

    @NotNull
    private Double totalPagar;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EstadoMensualidad estado;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    private String descripcion;
}
