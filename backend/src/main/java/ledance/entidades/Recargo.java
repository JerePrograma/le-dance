package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recargos")
public class Recargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String descripcion;

    @NotNull
    private Double porcentaje; // Porcentaje del recargo

    private Double valorFijo; // Valor fijo opcional del recargo

    @NotNull
    private Integer diaDelMesAplicacion; // ✅ Dia especifico del mes donde se aplica
}
