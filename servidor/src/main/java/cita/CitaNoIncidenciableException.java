package cita;

public class CitaNoIncidenciableException extends Exception {

    private static final long serialVersionUID = 1L;

    public CitaNoIncidenciableException(String mensaje) {
        super(mensaje);
    }
}
