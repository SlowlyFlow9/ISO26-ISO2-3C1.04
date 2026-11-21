package cita;

/**
 * CU25: la cita ya no se puede reprogramar ni cancelar
 * (está cancelada o realizada).
 */
public class CitaNoModificableException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public CitaNoModificableException(
            String mensaje) {
        super(mensaje);
    }
}
