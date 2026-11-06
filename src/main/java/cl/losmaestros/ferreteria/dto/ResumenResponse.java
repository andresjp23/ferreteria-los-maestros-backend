package cl.losmaestros.ferreteria.dto;

/**
 * Indicadores del dashboard del administrador.
 * Las variaciones comparan el mes actual con el anterior, en porcentaje (null si el mes anterior fue 0).
 */
public record ResumenResponse(
        long productos,
        long productosBajoStock,
        long usuarios,
        long pedidosMes,
        long ventasMes,
        Double variacionPedidos,
        Double variacionVentas) {
}
