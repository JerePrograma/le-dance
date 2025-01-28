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
@Table(name = "asistencias")
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private Boolean presente;

    private String observacion; // Comentarios como reemplazos, llegadas tarde, etc.

    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    @ManyToOne
    @JoinColumn(name = "profesor_id", nullable = true) // Opcional, si el profesor participa en la asistencia
    private Profesor profesor;

    @Column(nullable = false)
    private Boolean activo = true;
}
