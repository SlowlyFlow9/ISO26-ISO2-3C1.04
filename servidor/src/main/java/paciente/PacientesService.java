package paciente;

import java.util.List;

/** Contrato de la gestión de pacientes. Las reglas de negocio se validan siempre aquí (D-013). */
public interface PacientesService {

    /**
     * CU22: da de alta un paciente y devuelve el creado, con id y fechaAlta asignados y activo = true.
     *
     * @throws SinPermisoException si el rol del solicitante no es Personal administrativo (D-016)
     * @throws DatosInvalidosException si falta un campo obligatorio, o el DNI/NIE, el email o la fecha de nacimiento no son válidos
     * @throws PacienteDuplicadoException si el dni o la tarjeta sanitaria ya existen (D-006)
     */
    Paciente crear(Usuario solicitante, Paciente datos)
            throws SinPermisoException, DatosInvalidosException, PacienteDuplicadoException;


        /**
     * CU23: busca pacientes activos cuyo nombre, apellidos o DNI contengan el criterio (sin distinguir mayúsculas).
     * Un criterio vacío devuelve una lista vacía.
     */
    List<Paciente> buscar(Usuario solicitante, String criterio);

    /**
     * CU23: devuelve el paciente con ese id, esté activo o no.
     *
     * @throws PacienteNoEncontradoException si el id no existe
     */
    Paciente consultar(Usuario solicitante, long id) throws PacienteNoEncontradoException;

    /**
     * CU23: sustituye los datos editables del paciente (no cambian id, fechaAlta ni activo) y registra quién y cuándo (D-017).
     *
     * @throws SinPermisoException si el rol del solicitante no es Personal administrativo (D-016)
     * @throws PacienteNoEncontradoException si el id no existe
     * @throws PacienteYaInactivoException si el paciente está dado de baja (D-010)
     * @throws DatosInvalidosException si falta un campo obligatorio o el DNI/NIE o el email no son válidos
     * @throws PacienteDuplicadoException si el DNI o la tarjeta sanitaria pertenecen a otro paciente (D-006)
     */
    Paciente modificar(Usuario solicitante, long id, Paciente datos)
            throws SinPermisoException, PacienteNoEncontradoException, PacienteYaInactivoException,
                   DatosInvalidosException, PacienteDuplicadoException;
}

