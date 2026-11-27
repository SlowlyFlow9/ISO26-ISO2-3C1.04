package cita;

/**
 * CU04: el paciente intenta cancelar una cita sin respetar la antelación
 * mínima de cancelación de la política de citación (D-034).
 */
public class CancelacionFueraDePlazoException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public CancelacionFueraDePlazoException(
            String mensaje) {
        super(mensaje);
    }
}
