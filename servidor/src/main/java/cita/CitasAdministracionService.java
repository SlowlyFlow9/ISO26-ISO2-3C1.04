package cita;

import java.util.List;

import agenda.AgendaNoEncontradaException;
import agenda.HuecoAgendaNoEncontradoException;
import paciente.PacienteNoEncontradoException;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU25: gestión de las citas de un paciente por el personal administrativo.
 * Todas las operaciones exigen el rol PERSONAL_ADMINISTRATIVO (D-016 y D-023).
 */
public interface CitasAdministracionService {

    /**
     * Da cita a una solicitud pendiente de citación en un hueco libre.
     * La cita queda PROGRAMADA y la solicitud pasa a CITADA.
     */
    Cita asignar(
            Usuario solicitante,
            long solicitudId,
            long huecoId)
            throws SinPermisoException,
            SolicitudNoEncontradaException,
            SolicitudNoCitableException,
            PacienteNoEncontradoException,
            PacienteInactivoException,
            HuecoAgendaNoEncontradoException,
            AgendaNoEncontradaException,
            HuecoNoDisponibleException;

    /**
     * Mueve una cita PROGRAMADA o CONFIRMADA a otro hueco libre.
     * La cita queda PROGRAMADA y el hueco anterior queda libre.
     */
    Cita reprogramar(
            Usuario solicitante,
            long citaId,
            long nuevoHuecoId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoModificableException,
            HuecoAgendaNoEncontradoException,
            AgendaNoEncontradaException,
            HuecoNoDisponibleException;

    /**
     * Cancela una cita PROGRAMADA o CONFIRMADA y libera su hueco.
     * La solicitud de la que nació vuelve a PENDIENTE_CITACION.
     */
    Cita cancelar(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoModificableException;

    /** Lista todas las citas de un paciente. */
    List<Cita> listarCitasDePaciente(
            Usuario solicitante,
            long pacienteId)
            throws SinPermisoException,
            PacienteNoEncontradoException;

    /** Devuelve una cita por su identificador. */
    Cita consultarCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException;
}
