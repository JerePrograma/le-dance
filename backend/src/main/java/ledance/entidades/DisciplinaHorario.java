package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "disciplina_horarios")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DisciplinaHorario {

    private static final Logger log = LoggerFactory.getLogger(DisciplinaHorario.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "disciplina_id", nullable = false)
    @EqualsAndHashCode.Exclude  // Excluir para evitar recursión
    private Disciplina disciplina;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "dia")
    private DiaSemana diaSemana;

    @NotNull
    @Column(name = "horario_inicio")
    private LocalTime horarioInicio;

    @NotNull
    @Column(name = "duracion")
    private Double duracion;

    public void setDisciplina(Disciplina disciplina) {
        this.disciplina = disciplina;
        log.debug("Set disciplina: {}", (disciplina != null ? disciplina.getId() : "null"));
    }
}
