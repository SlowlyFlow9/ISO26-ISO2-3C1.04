package cita;

/**
 * CU25: el hueco elegido existe pero no se puede usar
 * (está bloqueado, su agenda no está activa o ya tiene una cita).
 */
public class HuecoNoDisponibleException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public HuecoNoDisponibleException(
            String mensaje) {
        super(mensaje);
    }
}
