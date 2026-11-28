package notificacion;

import java.util.List;

import cita.PacienteNoVinculadoException;
import paciente.SinPermisoException;
import paciente.Usuario;

public interface NotificacionesService {

    List<Notificacion> listarMisNotificaciones(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException;
}
