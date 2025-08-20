package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true) // clave: solo incluimos campos seguros
@Entity
@Table(name = "detalle_pagos",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"alumno_id", "matricula_id"}),
                @UniqueConstraint(columnNames = {"alumno_id", "mensualidad_id"})
        })
public class DetallePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Version
    private Long version;

    @Column(name = "descripcion_concepto")
    @ToString.Include
    private String descripcionConcepto;

    @ManyToOne
    @JoinColumn(name = "concepto_id")
    private Concepto concepto;
    @ManyToOne
    @JoinColumn(name = "subconcepto_id")
    private SubConcepto subConcepto;

    @Column(name = "cuota_o_cantidad")
    @ToString.Include
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

    @Column(name = "a_cobrar")
    @ToString.Include
    private Double aCobrar;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @ManyToOne
    @JoinColumn(name = "mensualidad_id")
    private Mensualidad mensualidad;

    @ManyToOne
    @JoinColumn(name = "matricula_id")
    private Matricula matricula;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private Stock stock;
    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(nullable = false)
    @ToString.Include
    private Boolean cobrado = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private TipoDetallePago tipo;

    @Column(name = "fecha_registro", nullable = false)
    @ToString.Include
    private LocalDate fechaRegistro;

    @Column(name = "tiene_recargo")
    private Boolean tieneRecargo = false;
    @Column(name = "es_clon")
    private Boolean esClon = false;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    @ToString.Include
    private EstadoPago estadoPago = EstadoPago.ACTIVO;

    @Column(name = "removido")
    private Boolean removido = false;

    @PrePersist
    public void prePersist() {
        normalizeFields();
    }

    @PreUpdate
    public void preUpdate() {
        normalizeFields();
    }

    private void normalizeFields() {
        if (this.pago != null && this.pago.getFecha() != null) this.fechaRegistro = this.pago.getFecha();
        else if (this.fechaRegistro == null) this.fechaRegistro = LocalDate.now();
        if (this.descripcionConcepto != null) this.descripcionConcepto = this.descripcionConcepto.trim().toUpperCase();
        if (this.matricula != null && (this.descripcionConcepto == null || this.descripcionConcepto.isBlank()))
            this.descripcionConcepto = "MATRICULA " + LocalDate.now().getYear();
    }
}
