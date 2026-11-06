package cl.losmaestros.ferreteria.service;

import cl.losmaestros.ferreteria.dto.LoginRequest;
import cl.losmaestros.ferreteria.dto.LoginResponse;
import cl.losmaestros.ferreteria.dto.UsuarioSesion;
import cl.losmaestros.ferreteria.exception.AccesoDenegadoException;
import cl.losmaestros.ferreteria.exception.NoAutorizadoException;
import cl.losmaestros.ferreteria.exception.RecursoNoEncontradoException;
import cl.losmaestros.ferreteria.model.Usuario;
import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import cl.losmaestros.ferreteria.security.JwtService;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Revisa las credenciales y entrega un JWT.
     * El mensaje es el mismo si el correo no existe o si la contraseña está mal,
     * para no revelar qué correos están registrados.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new NoAutorizadoException("Correo o contraseña incorrectos"));

        if (!usuario.estaActivo()) {
            throw new AccesoDenegadoException("Tu cuenta está inactiva. Contacta al administrador");
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        JwtService.TokenGenerado token = jwtService.generar(usuario);
        return new LoginResponse(token.token(), token.expiraEn(), UsuarioSesion.from(usuario));
    }

    /** Usuario dueño del token: el frontend lo usa al recargar la página para restaurar la sesión. */
    @Transactional(readOnly = true)
    public UsuarioSesion usuarioActual(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioSesion::from)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
