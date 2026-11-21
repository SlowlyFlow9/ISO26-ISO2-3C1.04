package cita;

/**
 * CU25: la solicitud no está pendiente de citación
 * (ya tiene cita, está realizada o fue anulada), D-026.
 */
public class SolicitudNoCitableException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public SolicitudNoCitableException(
            String mensaje) {
        super(mensaje);
    }
}
