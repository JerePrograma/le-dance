package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "profesores")
public class Profesor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String nombre;

    @NotNull
    private String apellido;

    /**
     * ✅ Nueva informacion personal
     */
    private LocalDate fechaNacimiento; // ✅ Fecha de nacimiento del profesor.

    private String telefono; // ✅ Numero de contacto principal.

    /**
     * ✅ Se almacenara en la BD y se actualizara automaticamente
     */
    private Integer edad;

    @OneToMany(mappedBy = "profesor")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude // ⬅️ Añadir aquí
    private List<Disciplina> disciplinas;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = true) // ✅ Relacion OPCIONAL
    private Usuario usuario; // Ahora puede ser nulo.

    @Column(nullable = false)
    private Boolean activo = true;

    //Agregar fecha cobro
}