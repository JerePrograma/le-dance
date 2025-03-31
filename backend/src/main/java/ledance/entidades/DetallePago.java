package ledance.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "detalle_pagos",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"alumno_id", "matricula_id"}),
                @UniqueConstraint(columnNames = {"alumno_id", "mensualidad_id"})
        }
)
public class DetallePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

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

    private Double valorBase;
    private Double importeInicial;
    private Double importePendiente;
    private Double aCobrar;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @ManyToOne
    @JoinColumn(name = "mensualidad_id", nullable = true)
    @ToString.Exclude // Evita recursividad con Mensualidad
    private Mensualidad mensualidad;

    @ManyToOne
    @JoinColumn(name = "matricula_id", nullable = true)
    private Matricula matricula;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = true)
    private Stock stock;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(nullable = false)
    private Boolean cobrado = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDetallePago tipo;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(name = "tiene_recargo")
    private Boolean tieneRecargo = false;

    @Column(name = "es_clon")
    private Boolean esClon = false;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;

    // Getters y Setters para aCobrar (u otros metodos personalizados)
    public Double getaCobrar() {
        return aCobrar;
    }

    public void setaCobrar(Double aCobrar) {
        this.aCobrar = aCobrar;
    }

    @PrePersist
    public void prePersist() {
        if (this.descripcionConcepto != null) {
            this.descripcionConcepto = this.descripcionConcepto.toUpperCase();
        }
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDate.now();
        }
        if (this.matricula != null && (this.descripcionConcepto == null || this.descripcionConcepto.trim().isEmpty())) {
            this.descripcionConcepto = "MATRICULA " + LocalDate.now().getYear();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.descripcionConcepto != null) {
            this.descripcionConcepto = this.descripcionConcepto.toUpperCase();
        }
        if (this.matricula != null && (this.descripcionConcepto == null || this.descripcionConcepto.trim().isEmpty())) {
            this.descripcionConcepto = "MATRICULA " + LocalDate.now().getYear();
        }
    }


}
