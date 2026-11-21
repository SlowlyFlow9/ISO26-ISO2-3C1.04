package cita;

/**
 * CU25: el hueco elegido no se puede usar (no existe, está bloqueado,
 * su agenda no está activa o ya tiene una cita).
 */
public class HuecoNoDisponibleException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public HuecoNoDisponibleException(
            String mensaje) {
        super(mensaje);
    }
}
