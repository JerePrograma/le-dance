package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "procesos_ejecutados")
public class ProcesoEjecutado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String proceso;

    private LocalDate ultimaEjecucion;

    // Constructor personalizado para inicializar sin id
    public ProcesoEjecutado(String proceso, LocalDate ultimaEjecucion) {
        this.proceso = proceso;
        this.ultimaEjecucion = ultimaEjecucion;
    }
}
