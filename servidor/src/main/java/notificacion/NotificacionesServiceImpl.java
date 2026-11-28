package notificacion;

import java.util.List;

import cita.IdentidadPaciente;
import cita.PacienteNoVinculadoException;
import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public class NotificacionesServiceImpl
        implements NotificacionesService {

    private final NotificacionesRepositorio repositorio;
    private final IdentidadPaciente identidadPaciente;

    public NotificacionesServiceImpl(
            NotificacionesRepositorio repositorio,
            IdentidadPaciente identidadPaciente) {
        this.repositorio = repositorio;
        this.identidadPaciente = identidadPaciente;
    }

    @Override
    public List<Notificacion> listarMisNotificaciones(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException {
        ControlAcceso.exigir(solicitante, Rol.PACIENTE);

        long pacienteId =
                identidadPaciente.obtenerPacienteId(solicitante);

        return repositorio.listarPorPaciente(pacienteId);
    }
}
