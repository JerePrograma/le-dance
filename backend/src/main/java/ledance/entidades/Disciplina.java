package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "disciplinas")
public class Disciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String nombre;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "disciplina_dias", joinColumns = @JoinColumn(name = "disciplina_id"))
    @Column(name = "dia")
    @Enumerated(EnumType.STRING)
    private Set<DiaSemana> diasSemana;

    private Integer frecuenciaSemanal;

    @NotNull
    private LocalTime horarioInicio;

    @NotNull
    private Double duracion;

    @ManyToOne
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @ManyToOne
    @JoinColumn(name = "profesor_id", nullable = false)
    @NotNull(message = "La disciplina debe tener un profesor asignado")
    private Profesor profesor;

    @ManyToOne
    @JoinColumn(name = "recargo_id")
    private Recargo recargo;

    @NotNull
    private Double valorCuota;

    // Valor de clase suelta y de clase prueba (para el cálculo de costo)
    private Double claseSuelta;

    // Descontar de la matrícula cuando la persona se inscribe (UNA ESPECIE DE SALDO A FAVOR)
    private Double clasePrueba;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones;
}
