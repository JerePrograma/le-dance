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
    private String proceso; // Por ejemplo: "RECARGO_DIA_15", "RECARGO_DIA_1", "MENSUALIDAD_AUTOMATICA"

    private LocalDate ultimaEjecucion;

    public ProcesoEjecutado(String proceso, LocalDate ultimaEjecucion) {
        this.proceso = proceso;
        this.ultimaEjecucion = ultimaEjecucion;
    }
}
