package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "detalle_pagos")
public class DetallePago {

    private static final Logger log = LoggerFactory.getLogger(DetallePago.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigoConcepto;

    @NotNull
    private String concepto;

    private String cuota;

    @ManyToOne
    @JoinColumn(name = "bonificacion_id")
    private Bonificacion bonificacion;

    @ManyToOne
    @JoinColumn(name = "recargo_id")
    private Recargo recargo;

    // Si no se envía, se inicializa en 0.0
    private Double aFavor = 0.0;

    @NotNull
    private Double valorBase;

    private Double abono;

    private Double importe;

    private Double aCobrar;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    // Getters y setters (generados por Lombok)

    // Consolidamos la lógica de cálculo en un único método público
    @PrePersist
    @PreUpdate
    public void calcularImporte() {
        log.info("Calculando importe para DetallePago id {} - Concepto: {}", this.id, this.concepto);
        double base = (valorBase != null ? valorBase : 0.0);
        double descuento = 0.0;
        if (bonificacion != null) {
            double descuentoFijo = (bonificacion.getValorFijo() != null ? bonificacion.getValorFijo() : 0.0);
            double descuentoPorcentaje = (bonificacion.getPorcentajeDescuento() != null ?
                    (bonificacion.getPorcentajeDescuento() / 100.0 * base) : 0.0);
            descuento = descuentoFijo + descuentoPorcentaje;
        }
        double recargoValor = (recargo != null ? obtenerValorRecargo() : 0.0);
        double favor = (aFavor != null ? aFavor : 0.0);

        // Si el usuario ya definió un valor para aCobrar, usamos ese valor
        if (aCobrar != null) {
            this.importe = base - aCobrar;
            this.abono = aCobrar;
        } else {
            double calculado = base - descuento + recargoValor - favor;
            BigDecimal bd = new BigDecimal(calculado).setScale(2, RoundingMode.HALF_UP);
            this.importe = bd.doubleValue();
            if (this.aCobrar == null && this.abono == null) {
                this.aCobrar = this.importe;
                this.abono = 0.0;
            } else if (this.aCobrar == null) {
                this.aCobrar = this.importe - this.abono;
            }
        }
        log.info("DetallePago recalculado: Importe = {}, Abono = {}, A Cobrar = {}", this.importe, this.abono, this.aCobrar);
    }

    private double obtenerValorRecargo() {
        if (recargo != null) {
            int diaActual = LocalDate.now().getDayOfMonth();
            if (!(diaActual == recargo.getDiaDelMesAplicacion())) {
                log.info("Hoy ({}) no es el día de aplicación del recargo ({}). No se aplica.", diaActual, recargo.getDiaDelMesAplicacion());
                return 0.0;
            }
            double recargoFijo = (recargo.getValorFijo() != null ? recargo.getValorFijo() : 0.0);
            double recargoPorcentaje = (recargo.getPorcentaje() != null ?
                    (recargo.getPorcentaje() / 100.0 * (valorBase != null ? valorBase : 0.0)) : 0.0);
            log.info("Aplicando recargo: fijo={} / porcentaje={} (Día Aplicación: {})",
                    recargoFijo, recargoPorcentaje, recargo.getDiaDelMesAplicacion());
            return recargoFijo + recargoPorcentaje;
        }
        log.info("No hay recargo aplicable, se toma 0");
        return 0.0;
    }
}
