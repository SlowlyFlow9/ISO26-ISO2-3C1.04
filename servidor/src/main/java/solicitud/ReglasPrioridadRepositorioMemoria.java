package solicitud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ReglasPrioridadRepositorioMemoria
        implements ReglasPrioridadRepositorio {

    private final Map<Long, ReglaPrioridad> datos =
            new LinkedHashMap<>();
    private long siguienteId = 1;

    @Override
    public synchronized ReglaPrioridad guardar(ReglaPrioridad regla) {
        if (regla == null) {
            throw new IllegalArgumentException(
                    "La regla de prioridad no puede ser nula");
        }
        if (regla.getId() < 0) {
            throw new IllegalArgumentException(
                    "El identificador no puede ser negativo");
        }

        ReglaPrioridad guardada = regla.getId() == 0
                ? regla.conId(siguienteId++)
                : regla;
        datos.put(guardada.getId(), guardada);
        siguienteId = Math.max(siguienteId, guardada.getId() + 1);
        return guardada;
    }

    @Override
    public synchronized Optional<ReglaPrioridad> buscarPorId(long id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public synchronized Optional<ReglaPrioridad> buscarPorNombre(
            String nombre) {
        String buscado = normalizar(nombre);
        return datos.values().stream()
                .filter(regla -> normalizar(regla.getNombre())
                        .equals(buscado))
                .findFirst();
    }

    @Override
    public synchronized Optional<ReglaPrioridad> buscarPorOrden(
            int orden) {
        return datos.values().stream()
                .filter(regla -> regla.getOrden() == orden)
                .findFirst();
    }

    @Override
    public synchronized List<ReglaPrioridad> todas() {
        return new ArrayList<>(datos.values());
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}
