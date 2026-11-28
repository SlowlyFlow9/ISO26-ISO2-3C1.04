package cita;

/**
 * CU03: el servidor rechaza el cambio porque incumple la política de
 * citación: plazo de la cita actual, antelación mínima o ventana máxima
 * del hueco nuevo (D-039).
 */
public class CambioFueraDePoliticaException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public CambioFueraDePoliticaException(
            String mensaje) {
        super(mensaje);
    }
}
