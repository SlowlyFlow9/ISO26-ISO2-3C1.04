package cita;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CitasRepositorioMemoria
        implements CitasRepositorio {

    private final Map<Long, Cita> citas =
            new LinkedHashMap<>();

    private long siguienteId = 1;

    @Override
    public synchronized Cita guardar(Cita cita) {

        if (cita.getId() == null) {
            cita.setId(siguienteId++);
        }

        citas.put(cita.getId(), cita);

        return cita;
    }

    @Override
    public synchronized Optional<Cita> buscarPorId(
            long id) {

        return Optional.ofNullable(
                citas.get(id));
    }

    @Override
    public synchronized List<Cita> listarPorPaciente(
            long pacienteId) {

        return citas.values().stream()
                .filter(cita ->
                        cita.getPacienteId()
                                == pacienteId)
                .toList();
    }

    @Override
    public synchronized Optional<Cita> buscarActivaPorHueco(
            long huecoId) {
        return citas.values().stream()
                .filter(cita ->
                        cita.getHuecoId() != null
                                && cita.getHuecoId() == huecoId)
                .filter(cita ->
                        cita.getEstado() == EstadoCita.PROGRAMADA
                                || cita.getEstado()
                                        == EstadoCita.CONFIRMADA)
                .findFirst();
    }
}
