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
    private String email;

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

    // Nuevo atributo para acumular credito de CLASE SUELTA
    @Column(name = "credito_acumulado", nullable = false)
    private Double creditoAcumulado = 0.0;

    @OneToMany(mappedBy = "alumno",
            cascade = CascadeType.ALL,
            orphanRemoval = true)      // ← para que clear() elimine las inscripciones
    @JsonIgnore
    private List<Inscripcion> inscripciones = new ArrayList<>();

    @OneToMany(mappedBy = "alumno",
            cascade = CascadeType.ALL,
            orphanRemoval = true)      // ← agregamos orphanRemoval
    @JsonIgnore
    private List<Matricula> matriculas = new ArrayList<>();
}
