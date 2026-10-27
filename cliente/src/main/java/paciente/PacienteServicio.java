package paciente;

/** Contrato con el servidor para la gestión de pacientes. La validación de negocio es siempre la del servidor (D-013). */
public interface PacienteServicio {

    /**
     * CU22: da de alta un paciente y devuelve el paciente registrado, con id y fechaAlta asignados.
     *
     * @throws PacienteDuplicadoException si el dni o la tarjeta sanitaria ya existen (D-006)
     */
    Paciente alta(Paciente paciente) throws PacienteDuplicadoException;
}
