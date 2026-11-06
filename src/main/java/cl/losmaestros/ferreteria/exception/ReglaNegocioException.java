package cl.losmaestros.ferreteria.exception;

/** Los datos tienen buen formato pero rompen una regla del negocio → 400. */
public class ReglaNegocioException extends RuntimeException {

    private final String campo;

    public ReglaNegocioException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
