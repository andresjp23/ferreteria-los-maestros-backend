package cl.losmaestros.ferreteria.dto;

import cl.losmaestros.ferreteria.model.EstadoUsuario;
import cl.losmaestros.ferreteria.model.Rol;
import cl.losmaestros.ferreteria.model.Usuario;
import java.time.LocalDateTime;

/** Usuario para el panel de administración. Nunca incluye la contraseña ni su hash. */
public record UsuarioResponse(Long id, String nombre, String email, Rol rol, EstadoUsuario estado,
        LocalDateTime ultimoAcceso, LocalDateTime creadoEn) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.getEstado(),
                u.getUltimoAcceso(), u.getCreadoEn());
    }
}
