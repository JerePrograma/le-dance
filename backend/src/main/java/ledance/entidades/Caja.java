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
@Table(name = "caja")
public class Caja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private Double totalEfectivo;

    @NotNull
    private Double totalTransferencia;
    
    @NotNull
    private Double totalTarjeta; // ✅ Agregado para conciliación con pago con tarjeta.

    private String rangoDesdeHasta;

    private String observaciones; // Informacion adicional

    @Column(nullable = false)
    private Boolean activo = true;
}
