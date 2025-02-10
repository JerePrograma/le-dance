package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @NotNull
    private Double matricula;

    private Double claseSuelta;
    private Double clasePrueba;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones;
}

