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
@Table(name = "reportes")
public class Reporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String tipo; // Ej.: Recaudacion, Asistencia, Pagos

    @NotNull
    private String descripcion;

    @NotNull
    private LocalDate fechaGeneracion;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(optional = true)
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;

}