package cl.losmaestros.ferreteria.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    /** Agrega el botón "Authorize" de Swagger: se pega el token del login y se envía en cada prueba. */
    @Bean
    public OpenAPI ferreteriaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ferretería Los Maestros API")
                        .description("API REST del proyecto semestral DSY1104: catálogo, usuarios y pedidos. "
                                + "Para los endpoints protegidos, inicia sesión en POST /api/auth/login y pega "
                                + "el token en Authorize.")
                        .version("0.0.1"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
