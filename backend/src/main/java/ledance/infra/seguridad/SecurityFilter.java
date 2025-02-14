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
import com.auth0.jwt.exceptions.JWTVerificationException;

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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.replace("Bearer ", "");
            try {
                String email = tokenService.getSubject(token);
                String tipo = tokenService.getTokenType(token);
                if (!"ACCESS".equals(tipo)) {
                    throw new JWTVerificationException("Token no es de tipo ACCESS");
                }
                var usuarioOpt = usuarioRepositorio.findByEmail(email);
                if (usuarioOpt.isPresent()) {
                    var userEntity = usuarioOpt.get();
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userEntity, null, userEntity.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JWTVerificationException ex) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}