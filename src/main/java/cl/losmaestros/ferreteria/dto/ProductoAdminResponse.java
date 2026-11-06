package cl.losmaestros.ferreteria.dto;

import cl.losmaestros.ferreteria.model.EstadoStock;
import cl.losmaestros.ferreteria.model.Producto;

/** Producto para el panel de administración: igual al público, más el precio de compra. */
public record ProductoAdminResponse(
        String id,
        String nombre,
        String categoria,
        String categoriaNombre,
        String subcategoria,
        String marca,
        String unidad,
        int precioCompra,
        int precio,
        int stock,
        int stockMinimo,
        String descripcion,
        String image,
        EstadoStock estadoStock) {

    public static ProductoAdminResponse from(Producto p) {
        return new ProductoAdminResponse(
                p.getCodigo(),
                p.getNombre(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getSubcategoria(),
                p.getMarca(),
                p.getUnidad(),
                p.getPrecioCompra(),
                p.getPrecio(),
                p.getStock(),
                p.getStockMinimo(),
                p.getDescripcion(),
                p.getImagen(),
                p.getEstadoStock());
    }
}
