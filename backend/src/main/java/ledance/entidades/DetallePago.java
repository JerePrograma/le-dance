package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @Column(name = "a_cobrar")
    private Double ACobrar;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "mensualidad_id", nullable = true)
    @ToString.Exclude
    private Mensualidad mensualidad;

    @ManyToOne(cascade = CascadeType.ALL)
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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    private EstadoPago estadoPago = EstadoPago.ACTIVO;

    @Column(name = "removido")
    private Boolean removido = false;

    /*
     * --------------------------
     * Callbacks JPA
     * --------------------------
     */

    @PrePersist
    public void prePersist() {
        // Ajuste fecha de registro
        if (this.pago != null && this.pago.getFecha() != null) {
            this.fechaRegistro = this.pago.getFecha();
        } else if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDate.now();
        }
        // Normalizar la descripción
        if (this.descripcionConcepto != null) {
            this.descripcionConcepto = this.descripcionConcepto.toUpperCase();
        }
        // Asignar descripción por defecto si es matrícula
        if (this.matricula != null && (this.descripcionConcepto == null || this.descripcionConcepto.trim().isEmpty())) {
            this.descripcionConcepto = "MATRICULA " + LocalDate.now().getYear();
        }
        // Llamada a la lógica para setear 'cuotaOCantidad'
        this.setCuotaFromDescripcion();
    }

    @PreUpdate
    public void preUpdate() {
        // Ajuste fecha de registro
        if (this.pago != null && this.pago.getFecha() != null) {
            this.fechaRegistro = this.pago.getFecha();
        }
        // Normalizar la descripción
        if (this.descripcionConcepto != null) {
            this.descripcionConcepto = this.descripcionConcepto.toUpperCase();
        }
        // Asignar descripción por defecto si es matrícula
        if (this.matricula != null && (this.descripcionConcepto == null || this.descripcionConcepto.trim().isEmpty())) {
            this.descripcionConcepto = "MATRICULA " + LocalDate.now().getYear();
        }
    }

    /**
     * Método privado para asignar el campo 'cuotaOCantidad'
     * según la lógica deseada:
     * - Si la descripción contiene "MATRICULA", usar lo posterior al primer espacio.
     * - Si la descripción contiene '-', usar lo posterior al primer guión.
     * - Caso contrario, usar "1".
     */
    private void setCuotaFromDescripcion() {
        if (this.descripcionConcepto == null || this.descripcionConcepto.trim().isEmpty()) {
            // Si la descripción está vacía, forzamos "1" por default.
            this.cuotaOCantidad = "1";
            return;
        }

        // Controlamos con indexOf y la cadena normalizada (ya en mayúsculas).
        String upperDesc = this.descripcionConcepto.toUpperCase();

        if (upperDesc.contains("MATRICULA")) {
            // Extraer lo que haya después del primer espacio
            int spaceIndex = upperDesc.indexOf(" ");
            if (spaceIndex >= 0 && spaceIndex < this.descripcionConcepto.length() - 1) {
                // Extraer la subcadena posterior al espacio
                String sub = this.descripcionConcepto.substring(spaceIndex + 1).trim();
                // sub contiene: "2025" en "MATRICULA 2025"
                this.cuotaOCantidad = sub.isEmpty() ? "1" : sub;
            } else {
                this.cuotaOCantidad = "1";
            }
        } else if (upperDesc.contains("-")) {
            int dashIndex = this.descripcionConcepto.indexOf("-");
            if (dashIndex >= 0 && dashIndex < this.descripcionConcepto.length() - 1) {
                String sub = this.descripcionConcepto.substring(dashIndex + 1).trim();
                // sub contiene: "CUOTA - MARZO DE 2025" en "DANZA - CUOTA - MARZO DE 2025"
                this.cuotaOCantidad = sub.isEmpty() ? "1" : sub;
            } else {
                this.cuotaOCantidad = "1";
            }
        } else {
            // Si no se detectan "MATRICULA" ni "-"
            this.cuotaOCantidad = "1";
        }
    }

}
