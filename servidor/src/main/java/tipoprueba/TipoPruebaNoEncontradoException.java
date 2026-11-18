package tipoprueba;

/** No existe el tipo de prueba solicitado. */
public class TipoPruebaNoEncontradoException extends ServicioException {
    private static final long serialVersionUID = 1L;

    public TipoPruebaNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
