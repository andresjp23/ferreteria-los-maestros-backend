package cl.losmaestros.ferreteria.service;

import cl.losmaestros.ferreteria.dto.ProductoAdminResponse;
import cl.losmaestros.ferreteria.dto.ProductoRequest;
import cl.losmaestros.ferreteria.dto.ProductoResponse;
import cl.losmaestros.ferreteria.exception.ConflictoException;
import cl.losmaestros.ferreteria.exception.RecursoNoEncontradoException;
import cl.losmaestros.ferreteria.exception.ReglaNegocioException;
import cl.losmaestros.ferreteria.model.Categoria;
import cl.losmaestros.ferreteria.model.Producto;
import cl.losmaestros.ferreteria.repository.CategoriaRepository;
import cl.losmaestros.ferreteria.repository.ProductoRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lógica del catálogo: consultas públicas y gestión de productos del administrador. */
@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // ---------- Catálogo público ----------

    /** Productos activos ordenados por nombre; si viene categoría, solo los de esa categoría. */
    public List<ProductoResponse> listar(String categoria) {
        List<Producto> productos = (categoria == null || categoria.isBlank())
                ? productoRepository.findByActivoTrueOrderByNombreAsc()
                : productoRepository.findByActivoTrueAndCategoriaIdOrderByNombreAsc(categoria.trim());
        return productos.stream().map(ProductoResponse::from).toList();
    }

    public ProductoResponse obtener(String codigo) {
        return ProductoResponse.from(buscarActivo(codigo));
    }

    public List<String> listarMarcas() {
        return productoRepository.findMarcasActivas();
    }

    public List<String> listarUnidades() {
        return productoRepository.findUnidadesActivas();
    }

    // ---------- Administración ----------

    public List<ProductoAdminResponse> listarAdmin() {
        return productoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(ProductoAdminResponse::from)
                .toList();
    }

    public ProductoAdminResponse obtenerAdmin(String codigo) {
        return ProductoAdminResponse.from(buscarActivo(codigo));
    }

    @Transactional
    public ProductoAdminResponse crear(ProductoRequest request) {
        String codigo = normalizarCodigo(request.codigo());

        // El código es la clave: no se puede repetir, aunque el producto esté desactivado
        if (productoRepository.existsById(codigo)) {
            throw new ConflictoException("codigo", "Ya existe un producto con este código");
        }

        Producto producto = new Producto(codigo);
        aplicarDatos(producto, request);
        return ProductoAdminResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public ProductoAdminResponse actualizar(String codigo, ProductoRequest request) {
        Producto producto = buscarActivo(codigo);

        if (!producto.getCodigo().equals(normalizarCodigo(request.codigo()))) {
            throw new ReglaNegocioException("codigo", "El código no se puede modificar");
        }

        aplicarDatos(producto, request);
        return ProductoAdminResponse.from(producto);
    }

    /** Desactiva el producto (borrado lógico): deja de verse, pero los pedidos antiguos lo conservan. */
    @Transactional
    public void eliminar(String codigo) {
        buscarActivo(codigo).setActivo(false);
    }

    // ---------- Apoyo ----------

    private Producto buscarActivo(String codigo) {
        return productoRepository.findByCodigoAndActivoTrue(normalizarCodigo(codigo))
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }

    /** Valida las reglas de negocio y copia los datos del formulario al producto. */
    private void aplicarDatos(Producto producto, ProductoRequest request) {
        // Regla de negocio: no se vende bajo el costo
        if (request.precioVenta() < request.precioCompra()) {
            throw new ReglaNegocioException("precioVenta", "No puede ser menor al precio de compra");
        }

        Categoria categoria = categoriaRepository.findById(request.categoria().trim())
                .orElseThrow(() -> new ReglaNegocioException("categoria", "Categoría no válida"));

        producto.setNombre(request.nombre().trim());
        producto.setCategoria(categoria);
        producto.setSubcategoria(request.subcategoria().trim());
        producto.setMarca(request.marca().trim());
        producto.setUnidad(request.unidad().trim());
        producto.setPrecioCompra(request.precioCompra());
        producto.setPrecio(request.precioVenta());
        producto.setStock(request.stock());
        producto.setStockMinimo(request.stockMinimo());
        producto.setDescripcion(textoOpcional(request.descripcion()));
        producto.setImagen(textoOpcional(request.imagen()));
    }

    private static String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }

    private static String textoOpcional(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
