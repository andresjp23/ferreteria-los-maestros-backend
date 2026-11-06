package cl.losmaestros.ferreteria.controller;

import cl.losmaestros.ferreteria.dto.CambioEstadoRequest;
import cl.losmaestros.ferreteria.dto.CrearPedidoRequest;
import cl.losmaestros.ferreteria.dto.PedidoResponse;
import cl.losmaestros.ferreteria.exception.ErrorResponse;
import cl.losmaestros.ferreteria.security.UsuarioAutenticado;
import cl.losmaestros.ferreteria.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos", description = "Checkout del cliente y gestión de pedidos del vendedor")
@ApiResponse(responseCode = "401", description = "Sin sesión",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "El rol no tiene permiso",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    @Operation(summary = "Lista los pedidos, del más reciente al más antiguo (vendedor y administrador)")
    public List<PedidoResponse> listar(
            @Parameter(description = "Cantidad máxima de pedidos (ej. 5 para el dashboard)")
            @RequestParam(required = false) Integer limit) {
        return pedidoService.listar(limit);
    }

    @GetMapping("/mios")
    @Operation(summary = "Pedidos del cliente con sesión")
    public List<PedidoResponse> misPedidos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pedidoService.misPedidos(usuario.id());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un pedido (vendedor y administrador)")
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crea un pedido con el carrito del cliente (checkout)",
            description = "El servidor toma el precio y el stock de la base de datos, descuenta el stock "
                    + "y calcula el total.")
    @ApiResponse(responseCode = "201", description = "Pedido creado")
    @ApiResponse(responseCode = "400", description = "Carrito vacío o cantidades inválidas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Un producto ya no existe o no tiene stock suficiente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PedidoResponse> crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CrearPedidoRequest request) {
        PedidoResponse creado = pedidoService.crear(usuario.id(), request);
        return ResponseEntity.created(URI.create("/api/pedidos/" + creado.id())).body(creado);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Avanza el estado del pedido (solo vendedor)",
            description = "En preparación → Enviado → Entregado. No se puede saltar ni retroceder.")
    @ApiResponse(responseCode = "400", description = "Transición no permitida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return pedidoService.cambiarEstado(id, request);
    }
}
