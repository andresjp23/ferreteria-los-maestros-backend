package cl.losmaestros.ferreteria.controller;

import cl.losmaestros.ferreteria.dto.ProductoResponse;
import cl.losmaestros.ferreteria.exception.ErrorResponse;
import cl.losmaestros.ferreteria.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Catálogo", description = "Consulta pública de productos, categorías, marcas y unidades")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Lista los productos activos",
            description = "Ordenados por nombre. La búsqueda, los filtros y el orden del catálogo se aplican en el frontend.")
    public List<ProductoResponse> listar(
            @Parameter(description = "Id de la categoría (ej. herramientas)")
            @RequestParam(required = false) String categoria) {
        return productoService.listar(categoria);
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Detalle de un producto")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "No existe o fue eliminado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProductoResponse obtener(@PathVariable String codigo) {
        return productoService.obtener(codigo);
    }
}
