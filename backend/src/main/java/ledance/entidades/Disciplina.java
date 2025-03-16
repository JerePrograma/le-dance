package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "disciplinas")
public class Disciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @ManyToOne
    @JoinColumn(name = "profesor_id", nullable = false)
    @NotNull(message = "La disciplina debe tener un profesor asignado")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude // ⬅️ Añadir aquí
    private Profesor profesor;

    @NotNull
    private Double valorCuota;

    private Double claseSuelta;
    private Double clasePrueba;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private List<Inscripcion> inscripciones;

    // Relacion con los horarios específicos
    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference   // <-- Añadido para evitar ciclo en la serializacion
    @EqualsAndHashCode.Exclude
    private List<DisciplinaHorario> horarios;

    public void addHorario(DisciplinaHorario horario) {
        horario.setDisciplina(this);
        this.horarios.add(horario);
    }
}
