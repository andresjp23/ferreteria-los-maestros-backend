package cl.losmaestros.ferreteria.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ferreteriaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Ferretería Los Maestros API")
                .description("API REST del proyecto semestral DSY1104: catálogo, usuarios y pedidos.")
                .version("0.0.1"));
    }
}
