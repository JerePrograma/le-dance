package ledance.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inscripciones")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con Alumno
    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

    // Relación con Disciplina
    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    // Relación opcional con la bonificación
    @ManyToOne
    @JoinColumn(name = "bonificacion_id", nullable = true)
    private Bonificacion bonificacion;

    private Double costoParticular;
    private String notas;
}
