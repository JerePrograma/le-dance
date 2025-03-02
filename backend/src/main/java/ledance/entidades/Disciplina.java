package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

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

    @ManyToOne
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @ManyToOne
    @JoinColumn(name = "profesor_id", nullable = false)
    @NotNull(message = "La disciplina debe tener un profesor asignado")
    private Profesor profesor;

    @NotNull
    private Double valorCuota;

    private Double claseSuelta;
    private Double clasePrueba;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Inscripcion> inscripciones;

    // Relación con horarios específicos
    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DisciplinaHorario> horarios;
}
