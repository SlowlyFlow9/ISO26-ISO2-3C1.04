package cita;

/**
 * CU03: el cambio de cita incumple la política de citación (D-034):
 * la cita actual está dentro del plazo mínimo de cancelación o el hueco
 * nuevo queda fuera de la antelación mínima o de la ventana máxima.
 */
public class CambioFueraDePoliticaException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public CambioFueraDePoliticaException(
            String mensaje) {
        super(mensaje);
    }
}
