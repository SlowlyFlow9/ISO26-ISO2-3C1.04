package agenda;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AgendasRepositorioMemoria
        implements AgendasRepositorio {

    private final Map<Long, AgendaMedica> agendas =
            new LinkedHashMap<>();

    private long siguienteId = 1;

    @Override
    public synchronized AgendaMedica guardar(
            AgendaMedica agenda) {

        if (agenda.getId() == null) {
            agenda.setId(siguienteId++);
        }

        agendas.put(agenda.getId(), agenda);

        return agenda;
    }

    @Override
    public synchronized Optional<AgendaMedica> buscarPorId(
            long id) {

        return Optional.ofNullable(agendas.get(id));
    }

    @Override
    public synchronized List<AgendaMedica> listar(
            Long medicoId,
            LocalDate fecha,
            Boolean activo) {

        return agendas.values().stream()
                .filter(a -> medicoId == null
                        || medicoId.equals(a.getMedicoId()))
                .filter(a -> fecha == null
                        || fecha.equals(a.getFecha()))
                .filter(a -> activo == null
                        || activo.equals(a.isActivo()))
                .toList();
    }
}