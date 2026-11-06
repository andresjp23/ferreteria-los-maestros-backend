package cl.losmaestros.ferreteria.dto;

import cl.losmaestros.ferreteria.validation.ContrasenaSegura;
import cl.losmaestros.ferreteria.validation.NombreCompleto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar un usuario (campos del formulario AddUserForm).
 * La contraseña es obligatoria al crear y opcional al editar: eso lo decide el servicio.
 */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @NombreCompleto
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 80, message = "No puede superar los 80 caracteres")
        @Pattern(regexp = "^\\s*[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}\\s*$",
                message = "Ingresa un correo válido (ej. nombre@dominio.cl)")
        String email,

        @NotBlank(message = "Selecciona un rol")
        @Pattern(regexp = "admin|vendedor|cliente", message = "Rol no válido")
        String rol,

        @NotBlank(message = "Selecciona un estado")
        @Pattern(regexp = "Activo|Inactivo", message = "Selecciona un estado")
        String estado,

        @ContrasenaSegura
        String password) {
}
