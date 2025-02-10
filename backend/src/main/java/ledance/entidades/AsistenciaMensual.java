package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    @OneToMany(mappedBy = "asistenciaMensual", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AsistenciaDiaria> asistenciasDiarias;

    // Cambiamos el Map por una lista de observaciones mensuales
    @OneToMany(mappedBy = "asistenciaMensual", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ObservacionMensual> observaciones;
}