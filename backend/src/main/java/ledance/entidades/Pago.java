package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"detallePagos", "pagoMedios"})
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private LocalDate fechaVencimiento;

    @NotNull
    @Min(value = 0, message = "El monto debe ser mayor o igual a 0")
    private Double monto;

    private Double valorBase;

    @NotNull
    @Min(value = 0, message = "El monto base no puede ser negativo")
    private Double importeInicial;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "metodo_pago_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MetodoPago metodoPago;

    @NotNull
    private Double saldoRestante = 0.0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    private EstadoPago estadoPago = EstadoPago.ACTIVO;

    private String observaciones;

    @OneToMany(mappedBy = "pago", cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH }, orphanRemoval = true)
    @JsonIgnore
    private List<DetallePago> detallePagos = new ArrayList<>();

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PagoMedio> pagoMedios;

    @NotNull
    @Column(name = "monto_pagado", nullable = false)
    @Min(value = 0, message = "El monto pagado no puede ser negativo")
    private Double montoPagado = 0.0;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;

    @PrePersist
    public void prePersist() {
        if (this.saldoRestante == null) {
            this.saldoRestante = 0.0;
        }
    }

}
