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
@Table(name = "bonificaciones")
public class Bonificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String descripcion; // p.ej., "1/2 BECA"

    @NotNull
    private Integer porcentajeDescuento; // p.ej., 50%

    // Nuevo atributo: valor fijo de descuento (opcional)
    private Double valorFijo;

    @Column(nullable = false)
    private Boolean activo = true;

    private String observaciones; // Campo opcional
}
