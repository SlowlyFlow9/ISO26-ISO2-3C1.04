package paciente;

/** El paciente está dado de baja y no admite la operación (D-010). */
public class PacienteYaInactivoException extends Exception {
    public PacienteYaInactivoException(String message) { super(message); }
}
