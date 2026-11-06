package cl.losmaestros.ferreteria.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de las pruebas de integración: levantan la aplicación completa con H2 y los datos iniciales.
 * Cada test se revierte al terminar (@Transactional), así no se afectan entre sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ApiTestBase {

    protected static final String ADMIN = "admin@losmaestros.cl";
    protected static final String VENDEDOR = "vendedor@losmaestros.cl";
    protected static final String CLIENTE = "cliente@losmaestros.cl";

    @Autowired
    protected MockMvc mockMvc;

    /** Inicia sesión con una cuenta demo y devuelve el header "Bearer <token>". */
    protected String bearer(String email) throws Exception {
        String password = email.substring(0, email.indexOf('@')) + "123";
        return bearer(email, password);
    }

    protected String bearer(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }
}
