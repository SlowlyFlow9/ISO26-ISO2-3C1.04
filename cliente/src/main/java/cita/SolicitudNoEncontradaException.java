package cita;

/**
 * CU25: no existe la solicitud de prueba a la que se quiere dar cita.
 */
public class SolicitudNoEncontradaException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public SolicitudNoEncontradaException(
            String mensaje) {
        super(mensaje);
    }
}
