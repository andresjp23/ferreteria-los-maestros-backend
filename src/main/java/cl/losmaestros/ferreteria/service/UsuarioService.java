package cl.losmaestros.ferreteria.service;

import cl.losmaestros.ferreteria.dto.UsuarioRequest;
import cl.losmaestros.ferreteria.dto.UsuarioResponse;
import cl.losmaestros.ferreteria.exception.ConflictoException;
import cl.losmaestros.ferreteria.exception.RecursoNoEncontradoException;
import cl.losmaestros.ferreteria.exception.ReglaNegocioException;
import cl.losmaestros.ferreteria.model.EstadoUsuario;
import cl.losmaestros.ferreteria.model.Rol;
import cl.losmaestros.ferreteria.model.Usuario;
import cl.losmaestros.ferreteria.repository.PedidoRepository;
import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión de cuentas desde el panel de administración. */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByIdAsc().stream().map(UsuarioResponse::from).toList();
    }

    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (request.password() == null || request.password().isEmpty()) {
            throw new ReglaNegocioException("password", "La contraseña es obligatoria");
        }

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("email", "Ya existe un usuario con este correo");
        }

        Usuario usuario = new Usuario();
        aplicarDatos(usuario, request, email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Edita un usuario. La contraseña solo cambia si viene con texto.
     * Regla: el administrador no puede quitarse a sí mismo el rol ni desactivar su propia cuenta,
     * para que el sistema nunca se quede sin quien lo administre.
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request, Long idActual) {
        Usuario usuario = buscar(id);

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictoException("email", "Ya existe un usuario con este correo");
        }

        if (id.equals(idActual)) {
            if (rol(request) != Rol.ADMIN) {
                throw new ReglaNegocioException("rol", "No puedes quitarte el rol de administrador");
            }
            if (estado(request) != EstadoUsuario.ACTIVO) {
                throw new ReglaNegocioException("estado", "No puedes desactivar tu propia cuenta");
            }
        }

        aplicarDatos(usuario, request, email);
        if (request.password() != null && !request.password().isEmpty()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UsuarioResponse.from(usuario);
    }

    /** Borra la cuenta. Si tiene pedidos no se puede (se perdería el historial): hay que desactivarla. */
    @Transactional
    public void eliminar(Long id, Long idActual) {
        if (id.equals(idActual)) {
            throw new ReglaNegocioException(null, "No puedes eliminar tu propia cuenta");
        }
        Usuario usuario = buscar(id);
        if (pedidoRepository.existsByClienteId(id)) {
            throw new ConflictoException(null, "El usuario tiene pedidos. Desactívalo en vez de eliminarlo");
        }
        usuarioRepository.delete(usuario);
    }

    // ---------- Apoyo ----------

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private void aplicarDatos(Usuario usuario, UsuarioRequest request, String email) {
        // Igual que buildUser del frontend: sin espacios en los bordes y uno solo entre palabras
        usuario.setNombre(request.nombre().trim().replaceAll("\\s+", " "));
        usuario.setEmail(email);
        usuario.setRol(rol(request));
        usuario.setEstado(estado(request));
    }

    // El DTO ya validó que rol y estado tienen un valor permitido
    private static Rol rol(UsuarioRequest request) {
        return Rol.desde(request.rol()).orElseThrow();
    }

    private static EstadoUsuario estado(UsuarioRequest request) {
        return EstadoUsuario.desde(request.estado()).orElseThrow();
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
