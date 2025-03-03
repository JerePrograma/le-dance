package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    // Se inicializa la lista para evitar null
    @OneToMany(mappedBy = "asistenciaMensual", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AsistenciaDiaria> asistenciasDiarias = new ArrayList<>();

    // Se elimina la lista de ObservacionMensual y se reemplaza por un único String.
    // Si en el pasado se usaba la lista para almacenar observaciones, ahora se guardará todo en este campo.
    private String observacion;
}
