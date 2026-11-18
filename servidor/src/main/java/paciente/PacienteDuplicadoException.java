package paciente;

/** El dni o la tarjeta sanitaria ya pertenecen a otro paciente (D-006). */
public class PacienteDuplicadoException extends Exception {
    public PacienteDuplicadoException(String message) { super(message); }
}
