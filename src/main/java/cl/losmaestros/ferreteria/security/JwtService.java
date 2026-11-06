package cl.losmaestros.ferreteria.security;

import cl.losmaestros.ferreteria.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Genera y valida los JWT de la API.
 * El token va firmado con HMAC-SHA256 usando una clave secreta que solo conoce el servidor:
 * si alguien modifica el contenido (por ejemplo, el rol), la firma deja de coincidir.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration duracion;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiracion-minutos}") long minutos) {
        // HS256 exige una clave de al menos 256 bits (32 bytes); si es más corta, Keys lanza un error al arrancar
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.duracion = Duration.ofMinutes(minutos);
    }

    /** Token para el usuario: sub = id, más el email y el rol (iat y exp los agrega la librería). */
    public TokenGenerado generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(duracion);

        String token = Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().getValor())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();

        return new TokenGenerado(token, expira);
    }

    /**
     * Verifica la firma y la fecha de vencimiento y devuelve el contenido del token.
     * Lanza ExpiredJwtException si venció, o JwtException si fue alterado o está mal formado.
     */
    public Claims validar(String token) throws JwtException {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }

    public record TokenGenerado(String token, Instant expiraEn) {
    }
}
