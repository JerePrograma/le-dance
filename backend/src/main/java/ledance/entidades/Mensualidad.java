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
    private LocalDate fechaGeneracion; // Fecha de generación de la mensualidad

    @NotNull
    private LocalDate fechaCuota; // Fecha del vencimiento de la mensualidad

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
    private Double totalPagar; // Calculado dinámicamente

    @NotNull
    @Enumerated(EnumType.STRING)
    private EstadoMensualidad estado;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    // Campo "descripcion" que se asignará desde el servicio
    private String descripcion;
}
