package agenda;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CU29 - Repositorio de huecos de agenda.
 *
 * Implementación en memoria para almacenar
 * y recuperar los huecos asociados a agendas médicas.
 */
public class HuecosAgendaRepositorioMemoria
        implements HuecosAgendaRepositorio {

    private final Map<Long, HuecoAgenda> huecos =
            new LinkedHashMap<>();

    private long siguienteId = 1;

    @Override
    public synchronized HuecoAgenda guardar(HuecoAgenda hueco) {

        if (hueco == null) {
            throw new IllegalArgumentException(
                    "El hueco no puede ser nulo");
        }

        if (hueco.getId() == null) {
            hueco.setId(siguienteId++);
        } else {
            if (hueco.getId() <= 0) {
                throw new IllegalArgumentException(
                        "El identificador debe ser positivo");
            }

            siguienteId = Math.max(
                    siguienteId, hueco.getId() + 1);
        }

        huecos.put(hueco.getId(), hueco);

        return hueco;
    }

    @Override
    public synchronized Optional<HuecoAgenda> buscarPorId(long id) {
        return Optional.ofNullable(huecos.get(id));
    }

    @Override
    public synchronized List<HuecoAgenda> listarPorAgenda(
            long agendaId) {

        return huecos.values().stream()
                .filter(h -> h.getAgendaId() == agendaId)
                .toList();
    }

}
