package notificacion;

import java.util.List;

public interface NotificacionesRepositorio {

    Notificacion guardar(Notificacion notificacion);

    List<Notificacion> listarPorPaciente(long pacienteId);
}
