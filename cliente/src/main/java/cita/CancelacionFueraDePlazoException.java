package cita;

/**
 * CU04: el servidor rechaza la cancelación porque no respeta la antelación
 * mínima de la política de citación (D-038).
 */
public class CancelacionFueraDePlazoException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public CancelacionFueraDePlazoException(
            String mensaje) {
        super(mensaje);
    }
}
