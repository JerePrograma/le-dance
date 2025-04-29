package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "inscripciones")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Alumno alumno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "disciplina_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Disciplina disciplina;

    @ManyToOne
    @JoinColumn(name = "bonificacion_id")
    private Bonificacion bonificacion;

    @NotNull
    private LocalDate fechaInscripcion;

    private LocalDate fechaBaja;

    @Enumerated(EnumType.STRING)
    @NotNull
    private EstadoInscripcion estado = EstadoInscripcion.ACTIVA;

    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private List<Mensualidad> mensualidades;

    @OneToMany(mappedBy="inscripcion",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<AsistenciaAlumnoMensual> asistenciasAlumnoMensual = new ArrayList<>();
}
