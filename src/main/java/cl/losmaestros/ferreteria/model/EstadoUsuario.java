package cl.losmaestros.ferreteria.model;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

/** Estado de una cuenta. Un usuario inactivo no puede iniciar sesión. */
public enum EstadoUsuario {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String valor;

    EstadoUsuario(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    public static Optional<EstadoUsuario> desde(String valor) {
        return Arrays.stream(values()).filter(e -> e.valor.equals(valor)).findFirst();
    }
}
