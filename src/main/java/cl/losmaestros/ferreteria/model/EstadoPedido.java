package cl.losmaestros.ferreteria.model;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

/** Estados de un pedido, en el orden en que avanza. En el JSON van con el texto del frontend. */
public enum EstadoPedido {
    EN_PREPARACION("En preparación"),
    ENVIADO("Enviado"),
    ENTREGADO("Entregado");

    private final String valor;

    EstadoPedido(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    /** Estado que sigue a este, o vacío si ya es el último. */
    public Optional<EstadoPedido> siguiente() {
        int i = ordinal() + 1;
        return i < values().length ? Optional.of(values()[i]) : Optional.empty();
    }

    public static Optional<EstadoPedido> desde(String valor) {
        return Arrays.stream(values()).filter(e -> e.valor.equals(valor)).findFirst();
    }
}
