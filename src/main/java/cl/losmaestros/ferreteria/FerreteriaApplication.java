package cl.losmaestros.ferreteria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Sin el usuario en memoria que Spring crea por defecto: la autenticación es propia, con JWT
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FerreteriaApplication {
    public static void main(String[] args) {
        SpringApplication.run(FerreteriaApplication.class, args);
    }
}
