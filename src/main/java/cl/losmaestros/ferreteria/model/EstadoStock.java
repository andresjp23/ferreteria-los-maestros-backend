package cl.losmaestros.ferreteria.model;

import com.fasterxml.jackson.annotation.JsonValue;

/** Estado de disponibilidad de un producto. Se serializa con el texto que muestra el frontend. */
public enum EstadoStock {
    EN_STOCK("En stock"),
    BAJO_STOCK("Bajo stock"),
    SIN_STOCK("Sin stock");

    private final String etiqueta;

    EstadoStock(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Sin stock si no quedan unidades; bajo stock si quedan igual o menos que el mínimo. */
    public static EstadoStock calcular(int stock, int stockMinimo) {
        if (stock <= 0) return SIN_STOCK;
        if (stock <= stockMinimo) return BAJO_STOCK;
        return EN_STOCK;
    }
}
