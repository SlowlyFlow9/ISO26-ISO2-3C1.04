
package cita;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface CitasService {

    // CU01: Consultar las citas del paciente
    List<Cita> listarMisCitas(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException;

    //CU01: Consultar el detalle de una cita propia.
    Cita consultarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException;

    //CU02: Confirmar una cita propia
    Cita confirmarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoConfirmableException;

    //CU04: Cancelar una cita propia
    Cita cancelarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoModificableException,
            CancelacionFueraDePlazoException;
}
