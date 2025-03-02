package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "disciplina_horarios")
public class DisciplinaHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "disciplina_id", nullable = false)
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
}
