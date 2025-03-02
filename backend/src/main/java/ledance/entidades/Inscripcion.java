package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
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
    private LocalDate fechaInscripcion;

    private LocalDate fechaBaja;

    @Enumerated(EnumType.STRING)
    @NotNull
    private EstadoInscripcion estado = EstadoInscripcion.ACTIVA;

    private String notas;

    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Pago> pagos;

    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AsistenciaMensual> asistenciasMensuales;

    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Mensualidad> mensualidades;

}
