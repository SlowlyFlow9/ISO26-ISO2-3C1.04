package cita;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.EstadoSolicitud;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorio;

public class CitasRealizacionServiceImpl
        implements CitasRealizacionService {

    private final CitasRepositorio citas;
    private final SolicitudesRepositorio solicitudes;

    public CitasRealizacionServiceImpl(
            CitasRepositorio citas,
            SolicitudesRepositorio solicitudes) {
        this.citas = citas;
        this.solicitudes = solicitudes;
    }

    @Override
    public synchronized Cita marcarRealizada(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoRealizableException,
            SolicitudNoEncontradaException {
        exigirIdentidadMedico(solicitante);

        Cita actual = buscarCitaPropia(solicitante, citaId);
        if (actual.getEstado() != EstadoCita.PROGRAMADA
                && actual.getEstado() != EstadoCita.CONFIRMADA) {
            throw new CitaNoRealizableException(
                    "La cita " + citaId + " está "
                            + actual.getEstado()
                            + " y no se puede marcar como realizada");
        }

        SolicitudPrueba solicitud = null;
        if (actual.getSolicitudId() != null) {
            solicitud = solicitudes.buscarPorId(actual.getSolicitudId())
                    .orElseThrow(() ->
                            new SolicitudNoEncontradaException(
                                    "No existe la solicitud vinculada a la cita "
                                            + citaId));
            if (solicitud.getEstado() != EstadoSolicitud.CITADA) {
                throw new CitaNoRealizableException(
                        "La solicitud vinculada a la cita no está citada");
            }
        }

        Cita realizada = actual.copia();
        realizada.setEstado(EstadoCita.REALIZADA);

        if (solicitud != null) {
            solicitudes.guardar(
                    solicitud.conEstado(EstadoSolicitud.REALIZADA));
        }

        return citas.guardar(realizada).copia();
    }

    private Cita buscarCitaPropia(
            Usuario solicitante,
            long citaId)
            throws CitaNoEncontradaException,
            SinPermisoException {
        Cita cita = citas.buscarPorId(citaId)
                .orElseThrow(() ->
                        new CitaNoEncontradaException(
                                "No existe la cita con id " + citaId));
        if (cita.getMedicoId() != solicitante.medicoId()) {
            throw new SinPermisoException(
                    "El médico solo puede gestionar sus propias citas");
        }
        return cita;
    }

    private void exigirIdentidadMedico(Usuario solicitante)
            throws SinPermisoException {
        ControlAcceso.exigir(solicitante, Rol.MEDICO);
        if (solicitante.medicoId() == null
                || solicitante.medicoId() <= 0) {
            throw new SinPermisoException(
                    "El identificador del médico es obligatorio");
        }
    }
}
