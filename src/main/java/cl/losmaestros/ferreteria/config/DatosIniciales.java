package cl.losmaestros.ferreteria.config;

import cl.losmaestros.ferreteria.model.Categoria;
import cl.losmaestros.ferreteria.model.EstadoUsuario;
import cl.losmaestros.ferreteria.model.Producto;
import cl.losmaestros.ferreteria.model.Rol;
import cl.losmaestros.ferreteria.model.Usuario;
import cl.losmaestros.ferreteria.repository.CategoriaRepository;
import cl.losmaestros.ferreteria.repository.ProductoRepository;
import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Carga los datos de demostración (resources/datos/*.json) al arrancar.
 * Es idempotente: cada tabla se llena solo si está vacía, así no se duplican datos ni se pisan cambios.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    /** Contraseña de las cuentas de ejemplo que no son las 3 cuentas demo del login. */
    private static final String PASSWORD_EJEMPLO = "Maestros2026";

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DatosIniciales(CategoriaRepository categoriaRepository, ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        cargarCategorias();
        cargarProductos();
        cargarUsuarios();
    }

    private void cargarUsuarios() throws IOException {
        if (usuarioRepository.count() > 0) return;

        List<UsuarioSemilla> semillas = leer("datos/usuarios.json", new TypeReference<>() {});
        List<Usuario> usuarios = semillas.stream().map(s -> {
            Usuario u = new Usuario();
            u.setNombre(s.nombre());
            u.setEmail(s.email());
            u.setRol(Rol.desde(s.rol()).orElseThrow());
            u.setEstado(EstadoUsuario.desde(s.estado()).orElseThrow());
            u.setUltimoAcceso(s.ultimoAcceso());
            u.setPasswordHash(passwordEncoder.encode(s.password() != null ? s.password() : PASSWORD_EJEMPLO));
            return u;
        }).toList();

        usuarioRepository.saveAll(usuarios);
        log.info("Datos iniciales: {} usuarios", usuarios.size());
    }

    private void cargarCategorias() throws IOException {
        if (categoriaRepository.count() > 0) return;

        List<CategoriaSemilla> semillas = leer("datos/categorias.json", new TypeReference<>() {});
        categoriaRepository.saveAll(semillas.stream().map(s -> new Categoria(s.id(), s.nombre())).toList());
        log.info("Datos iniciales: {} categorías", semillas.size());
    }

    private void cargarProductos() throws IOException {
        if (productoRepository.count() > 0) return;

        Map<String, Categoria> categorias = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getId, Function.identity()));

        List<ProductoSemilla> semillas = leer("datos/productos.json", new TypeReference<>() {});
        List<Producto> productos = semillas.stream().map(s -> {
            Producto p = new Producto(s.codigo());
            p.setNombre(s.nombre());
            p.setCategoria(categorias.get(s.categoria()));
            p.setSubcategoria(s.subcategoria());
            p.setMarca(s.marca());
            p.setUnidad(s.unidad());
            p.setPrecioCompra(s.precioCompra());
            p.setPrecio(s.precio());
            p.setStock(s.stock());
            p.setStockMinimo(s.stockMinimo());
            p.setDescripcion(s.descripcion());
            p.setImagen(s.imagen());
            return p;
        }).toList();

        productoRepository.saveAll(productos);
        log.info("Datos iniciales: {} productos", productos.size());
    }

    private <T> T leer(String ruta, TypeReference<T> tipo) throws IOException {
        try (InputStream in = new ClassPathResource(ruta).getInputStream()) {
            return objectMapper.readValue(in, tipo);
        }
    }

    private record CategoriaSemilla(String id, String nombre) {
    }

    private record UsuarioSemilla(String nombre, String email, String rol, String estado,
            LocalDateTime ultimoAcceso, String password) {
    }

    private record ProductoSemilla(String codigo, String nombre, String categoria, String subcategoria,
            String marca, String unidad, int precioCompra, int precio, int stock, int stockMinimo,
            String descripcion, String imagen) {
    }
}
