package cl.losmaestros.ferreteria.controller;

import cl.losmaestros.ferreteria.dto.CategoriaResponse;
import cl.losmaestros.ferreteria.service.CategoriaService;
import cl.losmaestros.ferreteria.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Listas de apoyo para filtros y formularios. */
@RestController
@RequestMapping("/api")
@Tag(name = "Catálogo")
public class CatalogoController {

    private final CategoriaService categoriaService;
    private final ProductoService productoService;

    public CatalogoController(CategoriaService categoriaService, ProductoService productoService) {
        this.categoriaService = categoriaService;
        this.productoService = productoService;
    }

    @GetMapping("/categorias")
    @Operation(summary = "Lista las categorías ordenadas por nombre")
    public List<CategoriaResponse> categorias() {
        return categoriaService.listar();
    }

    @GetMapping("/marcas")
    @Operation(summary = "Marcas de los productos activos, sin repetir")
    public List<String> marcas() {
        return productoService.listarMarcas();
    }

    @GetMapping("/unidades")
    @Operation(summary = "Unidades de venta de los productos activos, sin repetir")
    public List<String> unidades() {
        return productoService.listarUnidades();
    }
}
