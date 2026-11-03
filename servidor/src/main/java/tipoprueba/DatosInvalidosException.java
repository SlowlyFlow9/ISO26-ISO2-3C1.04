package tipoprueba;

/** Campos obligatorios vacíos o duración inválida. */
public class DatosInvalidosException extends ServicioException {
    private static final long serialVersionUID = 1L;

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
