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

    @OneToMany(mappedBy = "profesor")
    private List<Disciplina> disciplinas;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = true) // ✅ Relacion OPCIONAL
    private Usuario usuario; // Ahora puede ser nulo

    @Column(nullable = false)
    private Boolean activo = true;
}
