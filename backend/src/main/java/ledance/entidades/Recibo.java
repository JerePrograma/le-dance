package ledance.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recibos")
public class Recibo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;
    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private EstadoRecibo estado = EstadoRecibo.PENDIENTE;
    @Column(name = "storage_key", length = 500)
    private String storageKey;
    @Column(nullable = false)
    private Integer intentos = 0;
    @Column(length = 500)
    private String ultimoError;
    private Instant generadoAt;
    private Instant enviadoAt;
    @Version
    @Column(nullable = false)
    private Long version;
}
