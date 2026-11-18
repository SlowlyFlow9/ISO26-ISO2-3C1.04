package paciente;

/** No se pudo comunicar con el servidor o su respuesta no es la esperada. */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String message, Throwable cause) { super(message, cause); }

    public ServicioNoDisponibleException(String message) { super(message); }
}
