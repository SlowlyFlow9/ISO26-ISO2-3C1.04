package solicitud;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface ReglasPrioridadService {

    ReglaPrioridad crear(
            Usuario solicitante,
            DatosReglaPrioridad datos)
            throws SinPermisoException,
            DatosReglaPrioridadInvalidosException,
            ReglaPrioridadDuplicadaException;

    List<ReglaPrioridad> listar(Usuario solicitante)
            throws SinPermisoException;

    ReglaPrioridad consultar(Usuario solicitante, long id)
            throws SinPermisoException,
            ReglaPrioridadNoEncontradaException;

    ReglaPrioridad modificar(
            Usuario solicitante,
            long id,
            DatosReglaPrioridad datos)
            throws SinPermisoException,
            DatosReglaPrioridadInvalidosException,
            ReglaPrioridadDuplicadaException,
            ReglaPrioridadNoEncontradaException;

    void desactivar(Usuario solicitante, long id)
            throws SinPermisoException,
            ReglaPrioridadNoEncontradaException;
}
