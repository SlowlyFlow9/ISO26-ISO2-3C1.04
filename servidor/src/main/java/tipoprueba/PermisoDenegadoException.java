package tipoprueba;

/** El rol del usuario no permite la operación (D-016). */
public class PermisoDenegadoException extends ServicioException {
    private static final long serialVersionUID = 1L;

    public PermisoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
