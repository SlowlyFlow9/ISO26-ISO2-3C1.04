package cita;

import java.util.List;

import paciente.PacienteNoEncontradoException;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.SolicitudPrueba;

/**
 * CU25: contrato que consume el cliente para gestionar las citas de un
 * paciente (D-032). Las cinco primeras operaciones son las del servidor;
 * las dos últimas son lecturas para rellenar las listas de la pantalla.
 */
public interface CitasAdministracionService {

    /** Da cita a una solicitud pendiente de citación en un hueco libre. */
    Cita asignar(
            Usuario solicitante,
            long solicitudId,
            long huecoId)
            throws SinPermisoException,
            SolicitudNoEncontradaException,
            SolicitudNoCitableException,
            PacienteNoEncontradoException,
            PacienteInactivoException,
            HuecoNoDisponibleException;

    /** Mueve una cita PROGRAMADA o CONFIRMADA a otro hueco libre. */
    Cita reprogramar(
            Usuario solicitante,
            long citaId,
            long nuevoHuecoId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoModificableException,
            HuecoNoDisponibleException;

    /** Cancela una cita PROGRAMADA o CONFIRMADA y libera su hueco. */
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

    /** Lista las solicitudes de un paciente que están pendientes de citación. */
    List<SolicitudPrueba> listarSolicitudesPendientes(
            Usuario solicitante,
            long pacienteId)
            throws SinPermisoException,
            PacienteNoEncontradoException;

    /** Lista los huecos libres de las agendas activas. */
    List<HuecoDisponible> listarHuecosLibres(
            Usuario solicitante)
            throws SinPermisoException;
}
