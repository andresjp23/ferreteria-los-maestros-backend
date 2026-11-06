package cl.losmaestros.ferreteria.exception;

/** La persona está identificada pero no puede hacer esto (ej. cuenta inactiva) → 403. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
