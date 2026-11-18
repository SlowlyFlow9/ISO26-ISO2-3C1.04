package solicitud;

/**
 * CU13: la solicitud no esta pendiente de citacion, por lo que no se puede modificar ni anular (D-031).
 * El servidor HTTP la traduce a 409.
 */
public class SolicitudNoGestionableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SolicitudNoGestionableException(String mensaje) {
        super(mensaje);
    }
}
