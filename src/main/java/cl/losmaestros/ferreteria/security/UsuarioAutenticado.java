package cl.losmaestros.ferreteria.security;

import cl.losmaestros.ferreteria.model.Rol;
import cl.losmaestros.ferreteria.model.Usuario;

/** Quién hace la petición. Se obtiene en los controllers con @AuthenticationPrincipal. */
public record UsuarioAutenticado(Long id, String email, Rol rol) {

    public static UsuarioAutenticado from(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getRol());
    }
}
