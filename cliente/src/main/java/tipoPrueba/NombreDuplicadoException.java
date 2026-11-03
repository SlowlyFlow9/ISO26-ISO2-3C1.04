package tipoprueba;

/** El servicio rechaza el alta/modificación porque el nombre ya existe (D-009). */
public class NombreDuplicadoException extends ServicioException {
    private static final long serialVersionUID = 1L;

    public NombreDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
