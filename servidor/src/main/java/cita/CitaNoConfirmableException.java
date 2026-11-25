package cita;

/**
 * CU02 - Indica que una cita no puede confirmarse
 * debido a su estado actual.
 */

public class CitaNoConfirmableException extends Exception {

    public CitaNoConfirmableException(String mensaje) {
        super(mensaje);
    }
}
