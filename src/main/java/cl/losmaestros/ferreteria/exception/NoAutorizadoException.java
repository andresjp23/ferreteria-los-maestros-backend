package cl.losmaestros.ferreteria.exception;

/** Credenciales incorrectas → 401. */
public class NoAutorizadoException extends RuntimeException {

    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
