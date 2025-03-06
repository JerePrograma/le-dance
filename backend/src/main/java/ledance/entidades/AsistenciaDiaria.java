package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asistencias_diarias")
public class AsistenciaDiaria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EstadoAsistencia estado;

    // Se relaciona con el registro mensual de asistencia del alumno
    @ManyToOne
    @JoinColumn(name = "asistencia_alumno_mensual_id", nullable = false)
    private AsistenciaAlumnoMensual asistenciaAlumnoMensual;
}
