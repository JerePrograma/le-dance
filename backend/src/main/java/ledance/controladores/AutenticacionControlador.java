package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.request.LoginRequest;
import ledance.entidades.Usuario;
import ledance.infra.seguridad.TokenService;
import ledance.repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
public class AutenticacionControlador {

    private final AuthenticationManager authManager;
    private final TokenService tokenService;
    private final UsuarioRepositorio usuarioRepositorio;

    @Autowired
    public AutenticacionControlador(
            AuthenticationManager authManager,
            TokenService tokenService,
            UsuarioRepositorio usuarioRepositorio
    ) {
        this.authManager = authManager;
        this.tokenService = tokenService;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @PostMapping
    public ResponseEntity<?> realizarLogin(@RequestBody @Valid LoginRequest datos) {
        var authToken = new UsernamePasswordAuthenticationToken(datos.email(), datos.contrasena());
        var usuarioAutenticado = authManager.authenticate(authToken);

        var user = (Usuario) usuarioAutenticado.getPrincipal();

        // Generar tokens
        var accessToken = tokenService.generarAccessToken(user);
        var refreshToken = tokenService.generarRefreshToken(user);

        return ResponseEntity.ok(
                new TokensDTO(accessToken, refreshToken)
        );
    }

    // Este endpoint NO debe requerir un Access Token válido.
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody String refreshToken) {
        try {
            var email = tokenService.getSubject(refreshToken);
            var tokenType = tokenService.getTokenType(refreshToken);
            if (!"REFRESH".equals(tokenType)) {
                return ResponseEntity.status(401).body("El token no es de tipo REFRESH");
            }

            // Busca el usuario en la base de datos
            var usuario = usuarioRepositorio.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Verifica si el usuario esta activo
            if (!usuario.getActivo()) {
                return ResponseEntity.status(403).body("Usuario inactivo");
            }

            // Generar un nuevo Access Token
            var newAccess = tokenService.generarAccessToken(usuario);
            var newRefresh = tokenService.generarRefreshToken(usuario);

            return ResponseEntity.ok(new TokensDTO(newAccess, newRefresh));

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // DTO de respuesta con ambos tokens
    record TokensDTO(String accessToken, String refreshToken) {}
}
