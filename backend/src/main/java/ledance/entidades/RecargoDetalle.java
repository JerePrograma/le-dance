package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recargo_detalles")
public class RecargoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recargo_id", nullable = false)
    private Recargo recargo;

    @NotNull
    private Integer diaDesde; // ✅ Día a partir del cual se aplica el recargo.

    @NotNull
    private Double porcentaje; // ✅ Porcentaje del recargo.

}
