package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.request.LoginRequest;
import ledance.dto.usuario.response.UsuarioResponse;
import ledance.entidades.Usuario;
import ledance.infra.seguridad.TokenService;
import ledance.infra.seguridad.InvalidTokenException;
import ledance.infra.seguridad.TokenType;
import ledance.infra.seguridad.VerifiedToken;
import ledance.repositorios.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/login")
@Validated
public class AutenticacionControlador {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionControlador.class);
    private final AuthenticationManager authManager;
    private final TokenService tokenService;
    private final UsuarioRepositorio usuarioRepositorio;

    public AutenticacionControlador(AuthenticationManager authManager,
                                    TokenService tokenService,
                                    UsuarioRepositorio usuarioRepositorio) {
        this.authManager = authManager;
        this.tokenService = tokenService;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @PostMapping
    public ResponseEntity<LoginResponseDTO> realizarLogin(@RequestBody @Valid LoginRequest datos) {
        log.info("Intento de login para nombreUsuario: {}", datos.nombreUsuario());


        var authToken = new UsernamePasswordAuthenticationToken(datos.nombreUsuario(), datos.contrasena());
        var usuarioAutenticado = authManager.authenticate(authToken);
        var user = (Usuario) usuarioAutenticado.getPrincipal();
        if (!Boolean.TRUE.equals(user.getActivo())
                || user.getRol() == null
                || !Boolean.TRUE.equals(user.getRol().getActivo())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
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
    public ResponseEntity<LoginResponseDTO> refreshToken(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        try {
            String refreshToken = extractBearerToken(authorizationHeader);
            VerifiedToken verified = tokenService.verify(refreshToken, TokenType.REFRESH);
            var usuario = usuarioRepositorio.findById(verified.userId())
                    .filter(user -> Objects.equals(user.getNombreUsuario(), verified.subject()))
                    .filter(user -> Boolean.TRUE.equals(user.getActivo()))
                    .filter(user -> user.getRol() != null && Boolean.TRUE.equals(user.getRol().getActivo()))
                    .filter(user -> Objects.equals(user.getRol().getDescripcion(), verified.role()))
                    .orElseThrow(InvalidTokenException::new);
            var newAccess = tokenService.generarAccessToken(usuario);
            var newRefresh = tokenService.generarRefreshToken(usuario);
            var usuarioResponse = new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNombreUsuario(),
                    usuario.getRol().getDescripcion(),
                    usuario.getActivo()
            );

            return ResponseEntity.ok(new LoginResponseDTO(newAccess, newRefresh, usuarioResponse));
        } catch (InvalidTokenException e) {
            log.warn("Refresh token rechazado");
            return ResponseEntity.status(401).build();
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException();
        }
        String token = authorizationHeader.substring("Bearer ".length());
        if (token.isBlank()) {
            throw new InvalidTokenException();
        }
        return token;
    }

    public record LoginResponseDTO(
            String accessToken,
            String refreshToken,
            UsuarioResponse usuario
    ) {
    }

}
