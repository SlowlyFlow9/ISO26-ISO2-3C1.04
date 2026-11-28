package solicitud;

import java.util.ArrayList;
import java.util.List;

/**
 * CU28 - Historial de cambios de prioridad en memoria (D-024).
 */
public class CambiosPrioridadRepositorioMemoria implements CambiosPrioridadRepositorio {

    private final List<CambioPrioridad> cambios = new ArrayList<>();
    private long siguienteId = 1;

    @Override
    public synchronized CambioPrioridad guardar(CambioPrioridad cambio) {
        if (cambio == null) {
            throw new IllegalArgumentException("El cambio de prioridad no puede ser nulo");
        }
        if (cambio.id() < 0) {
            throw new IllegalArgumentException("El identificador no puede ser negativo");
        }
        CambioPrioridad guardado = cambio.id() == 0 ? cambio.conId(siguienteId++) : cambio;
        siguienteId = Math.max(siguienteId, guardado.id() + 1);
        cambios.add(guardado);
        return guardado;
    }

    @Override
    public synchronized List<CambioPrioridad> listarPorSolicitud(long solicitudId) {
        List<CambioPrioridad> resultado = new ArrayList<>();
        for (CambioPrioridad cambio : cambios) {
            if (cambio.solicitudId() == solicitudId) {
                resultado.add(0, cambio);
            }
        }
        return resultado;
    }
}
