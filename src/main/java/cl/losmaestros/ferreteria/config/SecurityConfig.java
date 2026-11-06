package cl.losmaestros.ferreteria.config;

import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import cl.losmaestros.ferreteria.security.JwtAuthFilter;
import cl.losmaestros.ferreteria.security.JwtService;
import cl.losmaestros.ferreteria.security.RestAuthHandlers;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad de la API: sin sesiones de servidor (cada petición trae su JWT),
 * y reglas de acceso por ruta y rol. Lo que no está listado exige sesión.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
            UsuarioRepository usuarioRepository, RestAuthHandlers authHandlers) throws Exception {
        http
                // CSRF protege sesiones basadas en cookies; esta API usa tokens en un header, así que no aplica
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authHandlers) // 401
                        .accessDeniedHandler(authHandlers))     // 403
                .authorizeHttpRequests(auth -> auth
                        // Preflight de CORS y documentación
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Público
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias", "/api/marcas",
                                "/api/unidades").permitAll()

                        // Solo administrador
                        .requestMatchers("/api/admin/**", "/api/usuarios/**").hasRole("ADMIN")

                        // Todo lo demás (ej. /api/auth/me): cualquier usuario con sesión
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(jwtService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Orígenes del frontend que pueden llamar a la API (CORS_ORIGINS, por defecto Vite en el 5173). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
