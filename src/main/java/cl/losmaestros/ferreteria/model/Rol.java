package cl.losmaestros.ferreteria.model;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

/** Roles del sistema. En el JSON van en minúsculas, como los usa el frontend. */
public enum Rol {
    ADMIN("admin"),
    VENDEDOR("vendedor"),
    CLIENTE("cliente");

    private final String valor;

    Rol(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    /** Autoridad de Spring Security: ROLE_ADMIN, ROLE_VENDEDOR, ROLE_CLIENTE. */
    public String autoridad() {
        return "ROLE_" + name();
    }

    public static Optional<Rol> desde(String valor) {
        return Arrays.stream(values()).filter(r -> r.valor.equals(valor)).findFirst();
    }
}
