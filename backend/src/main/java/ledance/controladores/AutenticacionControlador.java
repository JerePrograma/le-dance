package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.request.LoginRequest;
import ledance.dto.usuario.response.UsuarioResponse;
import ledance.entidades.Usuario;
import ledance.infra.seguridad.TokenService;
import ledance.repositorios.UsuarioRepositorio;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.notificaciones.NotificacionService;
import ledance.servicios.recargo.RecargoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/login")
@Validated
public class AutenticacionControlador {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionControlador.class);
    private final AuthenticationManager authManager;
    private final TokenService tokenService;
    private final UsuarioRepositorio usuarioRepositorio;
    private final RecargoServicio recargoServicio;
    private final MensualidadServicio mensualidadServicio;
    private final MatriculaServicio matriculaServicio;
    private final AsistenciaMensualServicio asistenciaMensualServicio;
    private final NotificacionService notificacionService;

    public AutenticacionControlador(AuthenticationManager authManager,
                                    TokenService tokenService,
                                    UsuarioRepositorio usuarioRepositorio, RecargoServicio recargoServicio, MensualidadServicio mensualidadServicio, MatriculaServicio matriculaServicio, AsistenciaMensualServicio asistenciaMensualServicio,
                                    NotificacionService notificacionService) {
        this.authManager = authManager;
        this.tokenService = tokenService;
        this.usuarioRepositorio = usuarioRepositorio;
        this.recargoServicio = recargoServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.matriculaServicio = matriculaServicio;
        this.asistenciaMensualServicio = asistenciaMensualServicio;
        this.notificacionService = notificacionService;
    }

    @PostMapping
    public ResponseEntity<?> realizarLogin(@RequestBody @Valid LoginRequest datos) {
        log.info("Intento de login para nombreUsuario: {}", datos.nombreUsuario());

        mensualidadServicio.generarMensualidadesParaMesVigente();
        matriculaServicio.generarMatriculasAnioVigente();
        recargoServicio.aplicarRecargosAutomaticos();
        asistenciaMensualServicio.crearAsistenciasParaInscripcionesActivasDetallado();

        // Actualizar/obtener notificaciones de cumpleaños (ahora usando la nueva lógica)
        List<String> cumpleaneros = notificacionService.generarYObtenerCumpleanerosDelDia();
        log.info("Cumpleañeros del día: {}", cumpleaneros);

        var authToken = new UsernamePasswordAuthenticationToken(datos.nombreUsuario(), datos.contrasena());
        var usuarioAutenticado = authManager.authenticate(authToken);
        var user = (Usuario) usuarioAutenticado.getPrincipal();
        var accessToken = tokenService.generarAccessToken(user);
        var refreshToken = tokenService.generarRefreshToken(user);

        var usuarioResponse = new UsuarioResponse(
                user.getId(),
                user.getNombreUsuario(),
                user.getRol().getDescripcion(),
                user.getActivo()
        );

        return ResponseEntity.ok(new LoginResponseDTO(accessToken, refreshToken, usuarioResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody String refreshToken) {
        try {
            var nombreUsuario = tokenService.getSubject(refreshToken);
            var tokenType = tokenService.getTokenType(refreshToken);
            if (!"REFRESH".equals(tokenType)) {
                return ResponseEntity.status(401).body("El token no es de tipo REFRESH");
            }
            var usuario = usuarioRepositorio.findByNombreUsuario(nombreUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            if (!usuario.getActivo()) {
                return ResponseEntity.status(403).body("Usuario inactivo");
            }
            var newAccess = tokenService.generarAccessToken(usuario);
            var newRefresh = tokenService.generarRefreshToken(usuario);
            var usuarioResponse = new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNombreUsuario(),
                    usuario.getRol().getDescripcion(), // Ajusta esto según tu modelo
                    usuario.getActivo()
            );

            return ResponseEntity.ok(new LoginResponseDTO(newAccess, newRefresh, usuarioResponse));
        } catch (RuntimeException e) {
            log.error("Error en refresh token: {}", e.getMessage());
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    public record LoginResponseDTO(
            String accessToken,
            String refreshToken,
            UsuarioResponse usuario
    ) {
    }

}
