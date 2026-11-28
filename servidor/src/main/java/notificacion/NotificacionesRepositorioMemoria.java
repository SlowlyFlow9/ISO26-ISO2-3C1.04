package notificacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotificacionesRepositorioMemoria
        implements NotificacionesRepositorio {

    private final Map<Long, Notificacion> notificaciones =
            new LinkedHashMap<>();

    @Override
    public synchronized Notificacion guardar(
            Notificacion notificacion) {
        if (notificacion == null) {
            throw new IllegalArgumentException(
                    "La notificación es obligatoria");
        }

        notificaciones.put(notificacion.id(), notificacion);
        return notificacion;
    }

    @Override
    public synchronized List<Notificacion> listarPorPaciente(
            long pacienteId) {
        return notificaciones.values().stream()
                .filter(notificacion ->
                        notificacion.pacienteId() == pacienteId)
                .toList();
    }
}
