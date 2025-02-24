package ledance.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "conceptos")
public class Concepto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descripcion;

    private double precio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sub_concepto_id", nullable = false)
    private SubConcepto subConcepto;
}
