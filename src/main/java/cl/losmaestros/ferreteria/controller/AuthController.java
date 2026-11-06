package cl.losmaestros.ferreteria.controller;

import cl.losmaestros.ferreteria.dto.LoginRequest;
import cl.losmaestros.ferreteria.dto.LoginResponse;
import cl.losmaestros.ferreteria.dto.UsuarioSesion;
import cl.losmaestros.ferreteria.exception.ErrorResponse;
import cl.losmaestros.ferreteria.security.UsuarioAutenticado;
import cl.losmaestros.ferreteria.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements // público: no pide token en Swagger
    @Operation(summary = "Inicia sesión y devuelve un JWT",
            description = "Cuentas demo: admin@losmaestros.cl / admin123, vendedor@losmaestros.cl / vendedor123, "
                    + "cliente@losmaestros.cl / cliente123")
    @ApiResponse(responseCode = "200", description = "Credenciales correctas")
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Cuenta inactiva",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario dueño del token (para restaurar la sesión al recargar)")
    @ApiResponse(responseCode = "401", description = "Sin token, token vencido o inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UsuarioSesion me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return authService.usuarioActual(usuario.id());
    }
}
