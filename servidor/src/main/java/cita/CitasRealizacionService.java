package cita;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface CitasRealizacionService {

    Cita marcarRealizada(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoRealizableException,
            SolicitudNoEncontradaException;
}
