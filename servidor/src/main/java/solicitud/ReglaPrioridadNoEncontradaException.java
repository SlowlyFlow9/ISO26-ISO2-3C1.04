package solicitud;

public class ReglaPrioridadNoEncontradaException extends Exception {

    private static final long serialVersionUID = 1L;

    public ReglaPrioridadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
