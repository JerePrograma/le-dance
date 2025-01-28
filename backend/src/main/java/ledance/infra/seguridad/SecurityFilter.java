package ledance.infra.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ledance.repositorios.UsuarioRepositorio;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepositorio usuarioRepositorio;

    public SecurityFilter(TokenService tokenService, UsuarioRepositorio usuarioRepositorio) {
        this.tokenService = tokenService;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            var token = authHeader.replace("Bearer ", "");
            try {
                var email = tokenService.getSubject(token); // Lanza excepción si expira
                // Revisamos también que sea un Access token (opcional)
                var tipo = tokenService.getTokenType(token);
                if (!"ACCESS".equals(tipo)) {
                    throw new RuntimeException("Token no es de tipo ACCESS");
                }

                //Si pasa, el Token es valido
                var usuarioOpt = usuarioRepositorio.findByEmail(email);
                if (usuarioOpt.isPresent()) {
                    var userEntity = usuarioOpt.get();
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userEntity, null, userEntity.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // Token expirado o inválido -> 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
