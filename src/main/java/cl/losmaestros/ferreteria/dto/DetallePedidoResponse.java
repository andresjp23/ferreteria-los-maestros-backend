package cl.losmaestros.ferreteria.dto;

import cl.losmaestros.ferreteria.model.DetallePedido;

public record DetallePedidoResponse(String codigo, String nombre, int precioUnitario, int cantidad, long subtotal) {

    public static DetallePedidoResponse from(DetallePedido d) {
        return new DetallePedidoResponse(d.getProducto().getCodigo(), d.getNombreProducto(), d.getPrecioUnitario(),
                d.getCantidad(), d.getSubtotal());
    }
}
