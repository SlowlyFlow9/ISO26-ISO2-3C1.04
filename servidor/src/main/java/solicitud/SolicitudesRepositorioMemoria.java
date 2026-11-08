package solicitud;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Almacena temporalmente las solicitudes en memoria.
 * CU11 - Prescribir pruebas diagnosticas.
 */
public class SolicitudesRepositorioMemoria implements SolicitudesRepositorio {

    private final Map <Long, SolicitudPrueba> solicitudes =
            new HashMap<>();

    private long siguienteId = 1;

    @Override
    public synchronized SolicitudPrueba guardar(
            SolicitudPrueba solicitud) {

        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }

        if (solicitud.getId() < 0) {
            throw new IllegalArgumentException(
                "El identificador no puede ser negativo");
        }

        SolicitudPrueba guardada = solicitud;

        if (solicitud.getId() == 0) {
            guardada = solicitud.conId(siguienteId++);
        } else {
            siguienteId = Math.max(
                siguienteId, solicitud.getId() + 1);
        }

        solicitudes.put(guardada.getId(), guardada);

        return guardada;
    }

    @Override
    public synchronized Optional<SolicitudPrueba> buscarPorId(
            long id) {

        return Optional.ofNullable(solicitudes.get(id));
    }
}
