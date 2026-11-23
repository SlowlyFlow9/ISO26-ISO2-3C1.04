package agenda;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface HorarioServicioService {

    HorarioServicio consultar(Usuario solicitante)
            throws SinPermisoException;

    HorarioServicio modificar(
            Usuario solicitante,
            HorarioServicio horario)
            throws SinPermisoException,
            DatosHorarioServicioInvalidosException;
}
