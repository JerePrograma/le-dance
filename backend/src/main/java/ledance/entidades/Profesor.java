package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "profesores")
public class Profesor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String nombre;

    @NotNull
    private String apellido;

    private String especialidad;

    private Integer aniosExperiencia;

    @OneToMany(mappedBy = "profesor")
    private List<Disciplina> disciplinas;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario; // Relacion opcional con un usuario autenticado.

    @Column(nullable = false)
    private Boolean activo = true;
}
