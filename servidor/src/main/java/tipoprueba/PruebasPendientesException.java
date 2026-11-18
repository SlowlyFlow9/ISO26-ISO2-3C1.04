package tipoprueba;

/** Se intenta desactivar un tipo con pruebas pendientes sin haberlo confirmado. */
public class PruebasPendientesException extends ServicioException {
    private static final long serialVersionUID = 1L;
    private final int pendientes;

    public PruebasPendientesException(int pendientes) {
        super("El tipo de prueba tiene " + pendientes
                + " prueba(s) pendiente(s). Confirma la desactivación para continuar.");
        this.pendientes = pendientes;
    }

    public int getPendientes() { return pendientes; }
}
