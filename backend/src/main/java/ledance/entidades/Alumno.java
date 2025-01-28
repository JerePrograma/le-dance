package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    private Integer edad; // Se recalcula en el servicio

    private String celular1;
    private String celular2;
    private String telefono;

    @Email
    private String email1;

    @Email
    private String email2;

    private String documento;
    private String cuit;

    @NotNull
    private LocalDate fechaIncorporacion;

    private String nombrePadres;
    private Boolean autorizadoParaSalirSolo;
    private String otrasNotas;

    private Double cuotaTotal; // Si deseas persistir la suma final

    @Column(nullable = false)
    private Boolean activo = true;
}
