package tipoprueba;

/** Error de negocio del servicio. Su mensaje es legible por el usuario. */
public class ServicioException extends Exception {
    private static final long serialVersionUID = 1L;

    public ServicioException(String mensaje) {
        super(mensaje);
    }
}
