package cl.losmaestros.ferreteria.dto;

import cl.losmaestros.ferreteria.model.EstadoPedido;
import cl.losmaestros.ferreteria.model.Pedido;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pedido con los campos que ya usa el panel del vendedor (id, cliente, fechaISO, estado, total, productos)
 * más el detalle de cada línea.
 */
public record PedidoResponse(
        Long id,
        String cliente,
        String clienteEmail,
        LocalDateTime fechaISO,
        EstadoPedido estado,
        long total,
        List<String> productos,
        List<DetallePedidoResponse> detalle) {

    public static PedidoResponse from(Pedido p) {
        return new PedidoResponse(
                p.getId(),
                p.getCliente().getNombre(),
                p.getCliente().getEmail(),
                p.getFecha(),
                p.getEstado(),
                p.getTotal(),
                p.getDetalles().stream().map(d -> d.getNombreProducto()).toList(),
                p.getDetalles().stream().map(DetallePedidoResponse::from).toList());
    }
}
