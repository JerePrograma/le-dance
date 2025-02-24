package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"detallePagos", "pagoMedios", "inscripcion"})
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

    @NotNull
    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    @ManyToOne
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private Boolean recargoAplicado = false;

    @Column(nullable = false)
    private Double bonificacionAplicada = 0.0;

    @NotNull
    private Double saldoRestante;

    @NotNull
    @Column(name = "saldo_a_favor", nullable = false)
    @Min(value = 0, message = "El saldo a favor no puede ser negativo")
    private Double saldoAFavor = 0.0;

    @Column(nullable = false)
    private Boolean activo = true;

    private String observaciones;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePago> detallePagos;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoMedio> pagoMedios;

    @PrePersist
    @PreUpdate
    public void actualizarImportes() {
        if (detallePagos != null) {
            for (DetallePago detalle : detallePagos) {
                detalle.calcularImporte();
            }
        }
    }

    public String getEstado() {
        return (activo != null && activo) ? "ACTIVO" : "ANULADO";
    }
}

//AGREGAR CORRECION DE COMPROBANTE. (MODIFICAR O ELIMINAR UN COMPROBANTE)
    //A LA HORA DE LISTARLO (RENDICIÓN) MOSTRARLO COMO ANULADO
    //BUSCAR TODOS LOS RECIBOS DEL ALUMNO
    //BOTÓN GENERAR CUOTAS A TODOS ALUMNOS ACTIVOS

    // Descontar de la matrícula cuando la persona se inscribe (UNA ESPECIE DE SALDO A FAVOR)

    // Que sea visible la Deuda para el cliente, (quiza donde está a favor)
    // Cuando paga un concepto en su totalidad que diga "SALDA ''El CONCEPTO''"
    // Cuando paga parcialmente un concepto que diga "A CUENTA ''EL CONCEPTO''"
    // Que cada vez que se acceda a una boleta
    // Quitar el "a favor" de totales y pago
    // Buscar todas las facturas con una lupita relacionadas a ese alumno. "Ver todas las facturas"
    // SI EL MÉTODO DE PAGO ES DEBITO, SE LE SUMA 5000
    // Cargar todas los items pendientes de la cobranza en una misma cobranza cuando se enlista el formulario de cobranza
    // Quitar recargo manualmente (Botón)

