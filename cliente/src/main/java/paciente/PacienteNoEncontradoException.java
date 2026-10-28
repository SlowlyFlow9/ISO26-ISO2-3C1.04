package paciente;

/** El id indicado no corresponde a ningún paciente. */
public class PacienteNoEncontradoException extends Exception {
    public PacienteNoEncontradoException(String message) { super(message); }
}
