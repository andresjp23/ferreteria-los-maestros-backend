package cl.losmaestros.ferreteria.controller;

import cl.losmaestros.ferreteria.dto.ProductoAdminResponse;
import cl.losmaestros.ferreteria.dto.ProductoRequest;
import cl.losmaestros.ferreteria.exception.ErrorResponse;
import cl.losmaestros.ferreteria.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de productos del panel de administración (incluye el precio de compra). */
@RestController
@RequestMapping("/api/admin/productos")
@Tag(name = "Admin · Productos", description = "Gestión del catálogo (solo administrador)")
public class AdminProductoController {

    private final ProductoService productoService;

    public AdminProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Lista los productos activos con su precio de compra")
    public List<ProductoAdminResponse> listar() {
        return productoService.listarAdmin();
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Obtiene un producto para editarlo")
    @ApiResponse(responseCode = "404", description = "No existe o fue eliminado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProductoAdminResponse obtener(@PathVariable String codigo) {
        return productoService.obtenerAdmin(codigo);
    }

    @PostMapping
    @Operation(summary = "Crea un producto")
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o precio de venta menor al de compra",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe un producto con ese código",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ProductoAdminResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoAdminResponse creado = productoService.crear(request);
        return ResponseEntity.created(URI.create("/api/admin/productos/" + creado.id())).body(creado);
    }

    @PutMapping("/{codigo}")
    @Operation(summary = "Actualiza un producto (el código no se puede cambiar)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe o fue eliminado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProductoAdminResponse actualizar(@PathVariable String codigo,
            @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(codigo, request);
    }

    @DeleteMapping("/{codigo}")
    @Operation(summary = "Elimina un producto (lo desactiva)")
    @ApiResponse(responseCode = "204", description = "Producto eliminado")
    @ApiResponse(responseCode = "404", description = "No existe o ya fue eliminado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
        productoService.eliminar(codigo);
        return ResponseEntity.noContent().build();
    }
}
