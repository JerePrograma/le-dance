package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    /** ✅ Nueva información personal */
    private LocalDate fechaNacimiento; // ✅ Fecha de nacimiento del profesor.

    private String telefono; // ✅ Número de contacto principal.

    /** ✅ Se almacenará en la BD y se actualizará automáticamente */
    private Integer edad;

    @OneToMany(mappedBy = "profesor")
    private List<Disciplina> disciplinas;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = true) // ✅ Relación OPCIONAL
    private Usuario usuario; // Ahora puede ser nulo.

    @Column(nullable = false)
    private Boolean activo = true;

}
