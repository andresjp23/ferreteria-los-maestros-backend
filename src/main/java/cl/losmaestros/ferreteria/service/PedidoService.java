package cl.losmaestros.ferreteria.service;

import cl.losmaestros.ferreteria.dto.CambioEstadoRequest;
import cl.losmaestros.ferreteria.dto.CrearPedidoRequest;
import cl.losmaestros.ferreteria.dto.PedidoResponse;
import cl.losmaestros.ferreteria.exception.ConflictoException;
import cl.losmaestros.ferreteria.exception.RecursoNoEncontradoException;
import cl.losmaestros.ferreteria.exception.ReglaNegocioException;
import cl.losmaestros.ferreteria.model.EstadoPedido;
import cl.losmaestros.ferreteria.model.Pedido;
import cl.losmaestros.ferreteria.model.Producto;
import cl.losmaestros.ferreteria.model.Usuario;
import cl.losmaestros.ferreteria.repository.PedidoRepository;
import cl.losmaestros.ferreteria.repository.ProductoRepository;
import cl.losmaestros.ferreteria.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pedidos: consulta del vendedor, cambio de estado y checkout del cliente. */
@Service
@Transactional(readOnly = true)
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Todos los pedidos, del más reciente al más antiguo. limit: cuántos devolver (ej. 5 para el dashboard). */
    public List<PedidoResponse> listar(Integer limit) {
        return pedidoRepository.findAllByOrderByFechaDesc().stream()
                .limit(limit != null && limit > 0 ? limit : Long.MAX_VALUE)
                .map(PedidoResponse::from)
                .toList();
    }

    public PedidoResponse obtener(Long id) {
        return PedidoResponse.from(buscar(id));
    }

    public List<PedidoResponse> misPedidos(Long clienteId) {
        return pedidoRepository.findByClienteIdOrderByFechaDesc(clienteId).stream()
                .map(PedidoResponse::from)
                .toList();
    }

    /**
     * Checkout. Todo ocurre en una sola transacción: si algo falla (un producto ya no existe o no hay
     * stock suficiente), no se crea el pedido ni se descuenta nada.
     */
    @Transactional
    public PedidoResponse crear(Long clienteId, CrearPedidoRequest request) {
        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        // Junta las cantidades si el mismo producto viene dos veces, conservando el orden del carrito
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        request.items().forEach(item ->
                cantidades.merge(item.id().trim().toUpperCase(Locale.ROOT), item.cantidad(), Integer::sum));

        // Bloquea los productos hasta el final de la transacción para que nadie más descuente su stock
        Map<String, Producto> productos = productoRepository.bloquearActivos(cantidades.keySet()).stream()
                .collect(Collectors.toMap(Producto::getCodigo, Function.identity()));

        Pedido pedido = new Pedido(cliente, LocalDateTime.now());
        cantidades.forEach((codigo, cantidad) -> {
            Producto producto = productos.get(codigo);
            if (producto == null) {
                throw new ConflictoException("items", "El producto " + codigo + " ya no está disponible");
            }
            if (producto.getStock() < cantidad) {
                throw new ConflictoException("items", "Stock insuficiente para \"" + producto.getNombre()
                        + "\": quedan " + producto.getStock() + " unidades");
            }
            producto.setStock(producto.getStock() - cantidad);
            pedido.agregarDetalle(producto, cantidad);
        });

        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    /** Regla de negocio: el estado solo avanza un paso (En preparación → Enviado → Entregado). */
    @Transactional
    public PedidoResponse cambiarEstado(Long id, CambioEstadoRequest request) {
        Pedido pedido = buscar(id);

        EstadoPedido nuevo = EstadoPedido.desde(request.estado())
                .orElseThrow(() -> new ReglaNegocioException("estado", "Estado no válido"));
        EstadoPedido actual = pedido.getEstado();

        EstadoPedido permitido = actual.siguiente()
                .orElseThrow(() -> new ReglaNegocioException("estado", "El pedido ya fue entregado"));
        if (nuevo != permitido) {
            throw new ReglaNegocioException("estado", "Un pedido \"" + actual.getValor()
                    + "\" solo puede pasar a \"" + permitido.getValor() + "\"");
        }

        pedido.setEstado(nuevo);
        return PedidoResponse.from(pedido);
    }

    private Pedido buscar(Long id) {
        return pedidoRepository.findWithDetallesById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
    }
}
