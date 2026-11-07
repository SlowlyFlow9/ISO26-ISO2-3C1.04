package prueba;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PruebasRepositorioMemoria implements PruebasRepositorio {
    private final Map<String, PruebaMedica> pruebas = new HashMap<>();

    @Override
    public void guardar(PruebaMedica prueba) {
        pruebas.put(prueba.getId(), prueba);
    }

    @Override
    public PruebaMedica obtenerPorId(String id) {
        return pruebas.get(id);
    }

    // <--- NUEVO MÉTODO
    @Override
    public List<PruebaMedica> obtenerTodas() {
        return new ArrayList<>(pruebas.values());
    }
}