package agenda;

/**
 * CU29 - Error en los datos de un hueco de agenda.
 */
public class DatosHuecoInvalidosException
        extends Exception {

    public DatosHuecoInvalidosException(String mensaje) {
        super(mensaje);
    }
}
