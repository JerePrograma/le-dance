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
@Table(name = "recargos")
public class Recargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String descripcion;

    @OneToMany(mappedBy = "recargo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecargoDetalle> detalles;

    // Método auxiliar para agregar un detalle
    public void addDetalle(RecargoDetalle detalle) {
        if (detalles == null) {
            detalles = new ArrayList<>();
        }
        detalles.add(detalle);
        detalle.setRecargo(this);
    }

    // Método auxiliar para remover un detalle
    public void removeDetalle(RecargoDetalle detalle) {
        if (detalles != null) {
            detalles.remove(detalle);
            detalle.setRecargo(null);
        }
    }
}