package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asistencias_mensuales")
public class AsistenciaMensual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Integer mes;

    @NotNull
    private Integer anio;

    @ManyToOne
    @JoinColumn(name = "disciplina_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Disciplina disciplina;

    // Nueva relacion: cada planilla tendra registros por alumno
    @OneToMany(mappedBy = "asistenciaMensual", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<AsistenciaAlumnoMensual> asistenciasAlumnoMensual = new ArrayList<>();
}
