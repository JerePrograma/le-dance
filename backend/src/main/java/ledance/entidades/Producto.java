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
@Table(name = "productos")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String nombre;

    @NotNull
    private Double precio;

    @ManyToOne
    private TipoProducto tipo; // INDUMENTARIA, ACCESORIO

    @NotNull
    private Integer stock;

    private Boolean requiereControlDeStock;

    private String codigoBarras;

    @Column(nullable = false)
    private Boolean activo = true;
}