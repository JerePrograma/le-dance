package ledance.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long usuarioId;

    @NotNull
    private String tipo; // Ejemplo: "CUMPLEANOS", "ALERTA", "MENSAJE", etc.

    @NotNull
    private String mensaje;

    @NotNull
    private LocalDateTime fechaCreacion;

    // Indica si ya fue leida (por defecto false)
    private boolean leida = false;
}
