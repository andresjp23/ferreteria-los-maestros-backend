package cl.losmaestros.ferreteria.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** Permisos por rol: lo que responde la API a cada tipo de usuario (evidencia de 401 y 403). */
class AutorizacionApiTest extends ApiTestBase {

    @Test
    void catalogoEsPublico() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categorias")).andExpect(status().isOk());
    }

    @Test
    void gestionDeProductosSoloAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/productos"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/productos").header("Authorization", bearer(CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes permisos para realizar esta acción"));
        mockMvc.perform(get("/api/admin/productos").header("Authorization", bearer(VENDEDOR)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/productos").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void gestionDeUsuariosSoloAdmin() throws Exception {
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(VENDEDOR)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void corsPermiteSoloAlFrontend() throws Exception {
        mockMvc.perform(options("/api/admin/productos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/productos")
                        .header("Origin", "http://sitio-ajeno.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
