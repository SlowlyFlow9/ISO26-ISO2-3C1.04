package cita;

public class CitaNoRealizableException extends Exception {

    private static final long serialVersionUID = 1L;

    public CitaNoRealizableException(String mensaje) {
        super(mensaje);
    }
}
