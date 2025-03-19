package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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

    // Usamos descripcionConcepto para registrar el nombre o descripción
    @Column(name = "descripcion_concepto")
    private String descripcionConcepto;

    @ManyToOne
    @JoinColumn(name = "concepto_id", nullable = true)
    private Concepto concepto;

    @ManyToOne
    @JoinColumn(name = "subconcepto_id", nullable = true)
    private SubConcepto subConcepto;

    private String cuota;

    @ManyToOne
    @JoinColumn(name = "bonificacion_id")
    private Bonificacion bonificacion;

    @ManyToOne
    @JoinColumn(name = "recargo_id")
    private Recargo recargo;

    // Monto abonado a favor (créditos, etc.)
    private Double aFavor = 0.0;

    // Monto original del concepto.
    @NotNull
    private Double montoOriginal;

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
        clon.setAlumno(this.getAlumno()); // O alternativamente: clon.setAlumno(nuevoPago.getAlumno());
        clon.setDescripcionConcepto(this.getDescripcionConcepto());
        clon.setConcepto(this.getConcepto());
        clon.setSubConcepto(this.getSubConcepto());
        clon.setCuota(this.getCuota());
        clon.setMontoOriginal(this.getMontoOriginal());
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

}
