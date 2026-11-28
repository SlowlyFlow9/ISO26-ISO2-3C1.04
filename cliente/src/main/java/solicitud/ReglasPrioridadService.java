package solicitud;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface ReglasPrioridadService {

    ReglaPrioridad crear(
            Usuario solicitante,
            DatosReglaPrioridad datos)
            throws SinPermisoException;

    List<ReglaPrioridad> listar(Usuario solicitante)
            throws SinPermisoException;

    ReglaPrioridad consultar(Usuario solicitante, long id)
            throws SinPermisoException;

    ReglaPrioridad modificar(
            Usuario solicitante,
            long id,
            DatosReglaPrioridad datos)
            throws SinPermisoException;

    void desactivar(Usuario solicitante, long id)
            throws SinPermisoException;
}
