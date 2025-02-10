package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inscripciones")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "disciplina_id", nullable = false)
    private Disciplina disciplina;

    @ManyToOne
    @JoinColumn(name = "bonificacion_id", nullable = true)
    private Bonificacion bonificacion;

    @NotNull
    private LocalDate fechaInscripcion; // ✅ Fecha de inicio

    private LocalDate fechaBaja; // ✅ Fecha de baja (si corresponde)

    @Enumerated(EnumType.STRING)
    @NotNull
    private EstadoInscripcion estado = EstadoInscripcion.ACTIVA; // ✅ Estado de la inscripción

    private String notas;

    /** ✅ Relación con pagos */
    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pago> pagos;

    /** ✅ Relación con asistencias mensuales */
    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AsistenciaMensual> asistenciasMensuales;
}
