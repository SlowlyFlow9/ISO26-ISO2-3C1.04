package paciente;

/** Contrato de la gestión de pacientes. Las reglas de negocio se validan siempre aquí (D-013). */
public interface PacienteServicio {

    /**
     * CU22: da de alta un paciente y devuelve el registrado, con id y fechaAlta asignados y activo = true.
     *
     * @throws IllegalArgumentException si falta algún campo obligatorio (D-021)
     * @throws PacienteDuplicadoException si el dni o la tarjeta sanitaria ya existen (D-006)
     */
    Paciente alta(Paciente paciente) throws PacienteDuplicadoException;
}
