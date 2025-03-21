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
@Table(name = "detalle_pagos")
public class DetallePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Campo de versión para control optimista
    @Version
    private Long version = 0L;

    // Usamos descripcionConcepto para registrar el nombre o descripción
    @Column(name = "descripcion_concepto")
    private String descripcionConcepto;

    @ManyToOne
    @JoinColumn(name = "concepto_id", nullable = true)
    private Concepto concepto;

    @ManyToOne
    @JoinColumn(name = "subconcepto_id", nullable = true)
    private SubConcepto subConcepto;

    @Column(name = "cuota_o_cantidad")
    private String cuotaOCantidad;

    @ManyToOne
    @JoinColumn(name = "bonificacion_id")
    private Bonificacion bonificacion;

    @ManyToOne
    @JoinColumn(name = "recargo_id")
    private Recargo recargo;

    // Monto original del concepto.
    private Double valorBase;

    // Importe inicial calculado en el momento de creación.
    private Double importeInicial;

    // Importe pendiente a abonar.
    private Double importePendiente;

    // Monto que se cobrará en el próximo abono.
    private Double aCobrar;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    // Relación opcional con Mensualidad (para cuando el tipo sea MENSUALIDAD).
    @ManyToOne
    @JoinColumn(name = "mensualidad_id", nullable = true)
    private Mensualidad mensualidad;

    // Relación opcional con Matricula (para cuando el tipo sea MATRICULA).
    @ManyToOne
    @JoinColumn(name = "matricula_id", nullable = true)
    private Matricula matricula;

    // Relación opcional con Stock (para cuando el tipo sea STOCK).
    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = true)
    private Stock stock;

    // Relación directa con Alumno para facilitar consultas y asignaciones
    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    // Indica si el detalle ya se ha saldado.
    @Column(nullable = false)
    private Boolean cobrado = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDetallePago tipo;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    // Getters y setters adicionales para aCobrar
    public Double getaCobrar() {
        return aCobrar;
    }

    public void setaCobrar(Double aCobrar) {
        this.aCobrar = aCobrar;
    }

    public DetallePago clonarConPendiente(Pago nuevoPago) {
        DetallePago clon = new DetallePago();
        clon.setAlumno(this.getAlumno());
        clon.setDescripcionConcepto(this.getDescripcionConcepto());
        clon.setConcepto(this.getConcepto());
        clon.setSubConcepto(this.getSubConcepto());
        clon.setValorBase(this.getValorBase());
        clon.setImporteInicial(this.getImportePendiente());
        clon.setaCobrar(0.0);
        clon.setImportePendiente(this.getImportePendiente());
        clon.setCobrado(false);
        clon.setBonificacion(this.getBonificacion());
        clon.setRecargo(this.getRecargo());
        clon.setTipo(this.getTipo());
        clon.setFechaRegistro(LocalDate.now());
        clon.setPago(nuevoPago);
        return clon;
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDate.now();
        }
    }

}
