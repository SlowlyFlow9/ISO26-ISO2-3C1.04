package tipoprueba;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Persistencia en memoria. Se sustituirá por la real sin tocar el servicio. */
public class RepositorioTiposPruebaMemoria implements RepositorioTiposPrueba {
    private final Map<Long, TipoPrueba> datos = new LinkedHashMap<>();
    private long siguienteId = 1;

    @Override
    public synchronized TipoPrueba guardar(TipoPrueba tipo) {
        TipoPrueba t = tipo.getId() == 0 ? tipo.conId(siguienteId++) : tipo;
        datos.put(t.getId(), t);
        return t;
    }

    @Override
    public synchronized Optional<TipoPrueba> buscarPorId(long id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public synchronized Optional<TipoPrueba> buscarPorNombre(String nombre) {
        String buscado = normalizar(nombre);
        for (TipoPrueba t : datos.values()) {
            if (normalizar(t.getNombre()).equals(buscado)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<TipoPrueba> todos() {
        return new ArrayList<>(datos.values());
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
