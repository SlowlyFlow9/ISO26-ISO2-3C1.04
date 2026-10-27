package paciente;

/** Contrato de la gestión de pacientes. Las reglas de negocio se validan siempre aquí (D-013). */
public interface PacientesService {

    /**
     * CU22: da de alta un paciente y devuelve el creado, con id y fechaAlta asignados y activo = true.
     *
     * @throws DatosInvalidosException si falta un campo obligatorio, o el DNI/NIE, el email o la fecha de nacimiento no son válidos
     * @throws PacienteDuplicadoException si el dni o la tarjeta sanitaria ya existen (D-006)
     */
    Paciente crear(Paciente datos) throws DatosInvalidosException, PacienteDuplicadoException;
}
