package cita;

/**
 * CU25: el paciente está dado de baja y no puede recibir citas (D-010).
 */
public class PacienteInactivoException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public PacienteInactivoException(
            String mensaje) {
        super(mensaje);
    }
}
