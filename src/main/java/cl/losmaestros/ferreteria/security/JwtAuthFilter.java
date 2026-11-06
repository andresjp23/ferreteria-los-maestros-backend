package cl.losmaestros.ferreteria.security;

import cl.losmaestros.ferreteria.model.Usuario;
import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Se ejecuta una vez por petición, antes de las reglas de autorización.
 * Si viene "Authorization: Bearer <token>" válido, deja al usuario autenticado con su rol.
 * Si el token venció o es inválido, no corta la petición: anota el motivo y sigue como anónimo.
 * Así una ruta pública funciona igual, y una protegida responde 401 con ese motivo (RestAuthHandlers).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Atributo de la petición donde se anota por qué no se aceptó el token. */
    public static final String MOTIVO_TOKEN = "jwt.motivo";

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIJO)) {
            try {
                Claims claims = jwtService.validar(header.substring(PREFIJO.length()));

                // Se lee el usuario de la BD: si lo desactivaron o le cambiaron el rol, se aplica de inmediato
                Optional<Usuario> usuario = usuarioRepository.findById(Long.valueOf(claims.getSubject()))
                        .filter(Usuario::estaActivo);

                if (usuario.isPresent()) {
                    autenticar(usuario.get());
                } else {
                    request.setAttribute(MOTIVO_TOKEN, "Tu cuenta no está disponible. Inicia sesión nuevamente");
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(MOTIVO_TOKEN, "Tu sesión expiró. Inicia sesión nuevamente");
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(MOTIVO_TOKEN, "Sesión inválida. Inicia sesión nuevamente");
            }
        }

        chain.doFilter(request, response);
    }

    private static void autenticar(Usuario usuario) {
        var autoridades = List.of(new SimpleGrantedAuthority(usuario.getRol().autoridad()));
        var autenticacion = new UsernamePasswordAuthenticationToken(
                UsuarioAutenticado.from(usuario), null, autoridades);
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
