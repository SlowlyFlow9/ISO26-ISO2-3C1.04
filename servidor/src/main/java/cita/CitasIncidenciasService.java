package cita;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface CitasIncidenciasService {

    IncidenciaCita registrarIncidencia(
            Usuario solicitante,
            long citaId,
            String descripcion)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoIncidenciableException;

    List<IncidenciaCita> listarIncidencias(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException;
}
