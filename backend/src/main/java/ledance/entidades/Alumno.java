package ledance.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alumnos")
@ToString(exclude = {"inscripciones", "matriculas"})
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String nombre;

    private String apellido;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private String celular1;
    private String celular2;

    @Email
    private String email1;
    @Email
    private String email2;

    private String documento;
    private String cuit;

    @NotNull
    private LocalDate fechaIncorporacion;
    private LocalDate fechaDeBaja;

    private Boolean deudaPendiente = false;
    private String nombrePadres;
    private Boolean autorizadoParaSalirSolo;
    private String otrasNotas;
    private Double cuotaTotal;

    @Column(nullable = false)
    private Boolean activo = true;

    // Relacion con inscripciones (ya definida)
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Inscripcion> inscripciones;

    // Nueva relacion con matrículas para que se eliminen en cascada
    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Matricula> matriculas;
}
