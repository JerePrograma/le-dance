package ledance.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asistencias_alumno_mensual")
public class AsistenciaAlumnoMensual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacion al alumno (a través de su inscripcion)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    // Observacion para este alumno en el mes
    private String observacion;

    // Relacion con la planilla mensual
    @ManyToOne
    @JoinColumn(name = "asistencia_mensual_id", nullable = false)
    private AsistenciaMensual asistenciaMensual;

    // Lista de asistencias diarias de este alumno en el mes
    @OneToMany(mappedBy = "asistenciaAlumnoMensual", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AsistenciaDiaria> asistenciasDiarias = new ArrayList<>();
}
