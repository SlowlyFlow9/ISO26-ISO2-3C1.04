package agenda;

/**
 * CU29 - Error al buscar un hueco inexistente.
 */
public class HuecoAgendaNoEncontradoException
        extends Exception {

    public HuecoAgendaNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
