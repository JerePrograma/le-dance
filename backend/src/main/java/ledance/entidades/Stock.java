package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String nombre;

    @NotNull
    private Double precio;

    @ManyToOne
    @JoinColumn(name = "tipo_stocks_id")
    private TipoStock tipo;

    @NotNull
    private Integer stock;

    private Boolean requiereControlDeStock;

    private String codigoBarras;

    @Column(nullable = false)
    private Boolean activo = true;

    // Fecha en la que se ingresa el stock
    private LocalDate fechaIngreso;

    // Fecha en la que se egresa (sale) el stock
    private LocalDate fechaEgreso;
}
