package cl.losmaestros.ferreteria.exception;

/** El dato choca con uno existente (código o correo repetido, stock insuficiente) → 409. */
public class ConflictoException extends RuntimeException {

    private final String campo;

    public ConflictoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
