package cl.losmaestros.ferreteria.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.losmaestros.ferreteria.model.EstadoUsuario;
import cl.losmaestros.ferreteria.model.Usuario;
import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import cl.losmaestros.ferreteria.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

/** Login, restauración de sesión (/me) y tratamiento de tokens vencidos o inválidos. */
class AuthApiTest extends ApiTestBase {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${app.jwt.secret}")
    private String secreto;

    private void login(String json, int status, String mensaje) throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.mensaje").value(mensaje));
    }

    @Test
    void loginCorrectoEntregaTokenYUsuarioSinContrasena() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  ADMIN@losmaestros.cl \",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiraEn").isString())
                .andExpect(jsonPath("$.usuario.email").value(ADMIN))
                .andExpect(jsonPath("$.usuario.rol").value("admin"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist())
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());
    }

    @Test
    void credencialesIncorrectasResponden401ConMensajeGenerico() throws Exception {
        login("{\"email\":\"admin@losmaestros.cl\",\"password\":\"otra\"}", 401, "Correo o contraseña incorrectos");
        login("{\"email\":\"nadie@losmaestros.cl\",\"password\":\"admin123\"}", 401, "Correo o contraseña incorrectos");
    }

    @Test
    void cuentaInactivaNoPuedeIniciarSesion() throws Exception {
        login("{\"email\":\"diego.silva@ejemplo.cl\",\"password\":\"Maestros2026\"}", 403,
                "Tu cuenta está inactiva. Contacta al administrador");
    }

    @Test
    void camposVaciosResponden400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").value("El correo es obligatorio"))
                .andExpect(jsonPath("$.campos.password").value("La contraseña es obligatoria"));
    }

    @Test
    void meDevuelveElUsuarioDelToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(VENDEDOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(VENDEDOR))
                .andExpect(jsonPath("$.rol").value("vendedor"));
    }

    @Test
    void meSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión para continuar"));
    }

    @Test
    void tokenAlteradoResponde401() throws Exception {
        String alterado = bearer(CLIENTE) + "x";
        mockMvc.perform(get("/api/auth/me").header("Authorization", alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Sesión inválida. Inicia sesión nuevamente"));
    }

    @Test
    void tokenVencidoResponde401() throws Exception {
        Usuario admin = usuarioRepository.findByEmail(ADMIN).orElseThrow();
        // Un JwtService con duración negativa genera tokens que ya nacen vencidos
        String vencido = new JwtService(secreto, -1).generar(admin).token();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Tu sesión expiró. Inicia sesión nuevamente"));

        // En una ruta pública el token vencido se ignora y la petición funciona igual
        mockMvc.perform(get("/api/productos").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioDesactivadoPierdeElAccesoAunqueSuTokenSigaVigente() throws Exception {
        String token = bearer(CLIENTE);
        usuarioRepository.findByEmail(CLIENTE).orElseThrow().setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.flush();

        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Tu cuenta no está disponible. Inicia sesión nuevamente"));
    }
}
