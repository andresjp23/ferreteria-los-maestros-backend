package cl.losmaestros.ferreteria.dto;

import cl.losmaestros.ferreteria.model.Rol;
import cl.losmaestros.ferreteria.model.Usuario;

/** Datos del usuario con sesión: lo mismo que hoy guarda el AuthProvider del frontend. */
public record UsuarioSesion(Long id, String nombre, String email, Rol rol) {

    public static UsuarioSesion from(Usuario u) {
        return new UsuarioSesion(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
    }
}
