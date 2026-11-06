package cl.losmaestros.ferreteria.dto;

import java.time.Instant;

/** token: JWT para el header Authorization. expiraEn: cuándo vence (ISO-8601, UTC). */
public record LoginResponse(String token, Instant expiraEn, UsuarioSesion usuario) {
}
