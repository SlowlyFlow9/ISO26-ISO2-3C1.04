package paciente;

/** El rol del usuario no permite la operación (D-016, D-023). */
public class SinPermisoException extends Exception {
    public SinPermisoException(String message) { super(message); }
}
