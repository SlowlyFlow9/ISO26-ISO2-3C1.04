package imagen;

public class SolicitudImagenNoEncontradaException
        extends Exception {

    public SolicitudImagenNoEncontradaException(
            String mensaje) {

        super(mensaje);
    }
}