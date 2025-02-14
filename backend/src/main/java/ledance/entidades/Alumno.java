package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alumnos")
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String nombre;

    private String apellido;

    private LocalDate fechaNacimiento;

    /**
     * ✅ Se almacenará en la BD y se actualizará automáticamente
     */
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

    private LocalDate fechaDeBaja; // ✅ NUEVO: Filtrar alumnos dados de baja

    private Boolean deudaPendiente = false; // ✅ NUEVO: Identificar si debe cuotas

    private String nombrePadres;
    private Boolean autorizadoParaSalirSolo;
    private String otrasNotas;

    private Double cuotaTotal; // Suma final de cuotas

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones;
}