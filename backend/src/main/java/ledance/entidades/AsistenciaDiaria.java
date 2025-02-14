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
@Table(name = "asistencias_diarias")
public class AsistenciaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @NotNull
    private EstadoAsistencia estado = EstadoAsistencia.AUSENTE;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "asistencia_mensual_id", nullable = false)
    private AsistenciaMensual asistenciaMensual;

    // ✅ Nuevo campo para observaciones
    private String observacion;
}