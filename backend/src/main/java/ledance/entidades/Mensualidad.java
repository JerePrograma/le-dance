package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mensualidades")
public class Mensualidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fechaGeneracion;

    @NotNull
    private LocalDate fechaCuota;

    // Fecha en la que se realizo el pago (null si aun no se paga)
    private LocalDate fechaPago;

    @NotNull
    @Min(value = 0, message = "El valor base debe ser mayor o igual a 0")
    private Double valorBase;

    @ManyToOne
    @JoinColumn(name = "recargo_id", nullable = true)
    private Recargo recargo;

    @ManyToOne
    @JoinColumn(name = "bonificacion_id", nullable = true)
    private Bonificacion bonificacion;

    @NotNull
    @Column(name="importe_inicial")
    private Double importeInicial;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EstadoMensualidad estado;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Inscripcion inscripcion;

    private String descripcion;

    // Nuevo campo para registrar el monto acumulado abonado en la mensualidad
    @NotNull
    private Double montoAbonado = 0.0;

    @OneToMany(mappedBy = "mensualidad", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // Evita la recursividad
    private List<DetallePago> detallePagos;

    private Double importePendiente;
}
