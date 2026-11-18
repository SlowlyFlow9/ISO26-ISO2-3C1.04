package tipoprueba;

/** Error de negocio devuelto por el servicio; su mensaje se muestra al usuario. */
public class ServicioException extends Exception {
    private static final long serialVersionUID = 1L;

    public ServicioException(String mensaje) {
        super(mensaje);
    }
}
