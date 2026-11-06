package cl.losmaestros.ferreteria.dto;

import cl.losmaestros.ferreteria.model.EstadoStock;
import cl.losmaestros.ferreteria.model.Producto;

/**
 * Producto para el catálogo público. Usa los mismos nombres que el frontend
 * (id = código, image = ruta de la imagen) y NO incluye el precio de compra.
 */
public record ProductoResponse(
        String id,
        String nombre,
        String categoria,
        String categoriaNombre,
        String subcategoria,
        String marca,
        String unidad,
        int precio,
        int stock,
        int stockMinimo,
        String descripcion,
        String image,
        EstadoStock estadoStock) {

    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
                p.getCodigo(),
                p.getNombre(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getSubcategoria(),
                p.getMarca(),
                p.getUnidad(),
                p.getPrecio(),
                p.getStock(),
                p.getStockMinimo(),
                p.getDescripcion(),
                p.getImagen(),
                p.getEstadoStock());
    }
}
