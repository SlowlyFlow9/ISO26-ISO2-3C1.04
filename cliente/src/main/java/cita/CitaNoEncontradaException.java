package cita;

public class CitaNoEncontradaException
        extends Exception {

    public CitaNoEncontradaException(
            String mensaje) {

        super(mensaje);
    }
}