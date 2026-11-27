package cita;

import java.util.ArrayList;
import java.util.List;

public class IncidenciasRepositorioMemoria
        implements IncidenciasRepositorio {

    private final List<IncidenciaCita> incidencias =
            new ArrayList<>();

    private long siguienteId = 1;

    @Override
    public synchronized IncidenciaCita guardar(
            IncidenciaCita incidencia) {
        if (incidencia == null) {
            throw new IllegalArgumentException(
                    "La incidencia es obligatoria");
        }

        IncidenciaCita guardada = incidencia.getId() == null
                ? incidencia.conId(siguienteId++)
                : incidencia;
        incidencias.add(guardada);
        return guardada;
    }

    @Override
    public synchronized List<IncidenciaCita> listarPorCita(
            long citaId) {
        return incidencias.stream()
                .filter(incidencia ->
                        incidencia.getCitaId() == citaId)
                .toList();
    }
}
