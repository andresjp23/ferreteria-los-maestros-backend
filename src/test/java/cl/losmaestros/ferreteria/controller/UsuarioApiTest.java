package cl.losmaestros.ferreteria.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** CRUD de usuarios del panel de administración. */
class UsuarioApiTest extends ApiTestBase {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String admin;

    @BeforeEach
    void iniciarSesion() throws Exception {
        admin = bearer(ADMIN);
    }

    private static String usuario(String nombre, String email, String rol, String estado, String password) {
        String pass = password == null ? "null" : "\"" + password + "\"";
        return """
                {"nombre":"%s","email":"%s","rol":"%s","estado":"%s","password":%s}
                """.formatted(nombre, email, rol, estado, pass);
    }

    private ResultActions crear(String json) throws Exception {
        return mockMvc.perform(post("/api/usuarios").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Long idDe(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow().getId();
    }

    @Test
    void listaLosUsuariosSinContrasenas() throws Exception {
        mockMvc.perform(get("/api/usuarios").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(42)))
                .andExpect(jsonPath("$[0].email").value(ADMIN))
                .andExpect(jsonPath("$[0].rol").value("admin"))
                .andExpect(jsonPath("$[0].estado").value("Activo"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void crearUsuarioYLuegoIniciarSesionConEl() throws Exception {
        crear(usuario("  María   José Pérez ", "MJ.Perez@Ejemplo.cl", "vendedor", "Activo", "Segura2026"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("María José Pérez"))
                .andExpect(jsonPath("$.email").value("mj.perez@ejemplo.cl"))
                .andExpect(jsonPath("$.rol").value("vendedor"));

        bearer("mj.perez@ejemplo.cl", "Segura2026");
    }

    @Test
    void crearSinContrasenaResponde400() throws Exception {
        crear(usuario("Ana Rojas", "ana@ejemplo.cl", "cliente", "Activo", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.password").value("La contraseña es obligatoria"));
    }

    @Test
    void validacionesConLosMensajesDelFormulario() throws Exception {
        crear(usuario("Ana", "no-es-correo", "jefe", "Activo", "segura2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").value("Ingresa nombre y apellido"))
                .andExpect(jsonPath("$.campos.email").value("Ingresa un correo válido (ej. nombre@dominio.cl)"))
                .andExpect(jsonPath("$.campos.rol").value("Rol no válido"))
                .andExpect(jsonPath("$.campos.password").value("Debe incluir al menos una letra mayúscula"));
    }

    @Test
    void correoRepetidoResponde409() throws Exception {
        crear(usuario("Otro Admin", "ADMIN@losmaestros.cl", "admin", "Activo", "Segura2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos.email").value("Ya existe un usuario con este correo"));
    }

    @Test
    void editarSinContrasenaMantieneLaAnterior() throws Exception {
        Long id = idDe(VENDEDOR);
        mockMvc.perform(put("/api/usuarios/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuario("Pedro Soto Muñoz", VENDEDOR, "vendedor", "Activo", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pedro Soto Muñoz"));

        bearer(VENDEDOR); // vendedor123 sigue funcionando
    }

    @Test
    void elAdminNoPuedeQuitarseElRolNiDesactivarse() throws Exception {
        Long id = idDe(ADMIN);
        mockMvc.perform(put("/api/usuarios/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuario("Administrador General", ADMIN, "vendedor", "Activo", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.rol").value("No puedes quitarte el rol de administrador"));

        mockMvc.perform(put("/api/usuarios/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuario("Administrador General", ADMIN, "admin", "Inactivo", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.estado").value("No puedes desactivar tu propia cuenta"));
    }

    @Test
    void eliminarUsuario() throws Exception {
        String body = crear(usuario("Temporal Prueba", "temporal@ejemplo.cl", "cliente", "Activo", "Segura2026"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        mockMvc.perform(delete("/api/usuarios/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/usuarios/" + id).header("Authorization", admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void elAdminNoPuedeEliminarseASiMismo() throws Exception {
        mockMvc.perform(delete("/api/usuarios/" + idDe(ADMIN)).header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No puedes eliminar tu propia cuenta"));
    }
}
