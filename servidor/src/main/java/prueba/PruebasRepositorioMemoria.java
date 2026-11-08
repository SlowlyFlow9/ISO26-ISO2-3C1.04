package prueba;

import java.util.HashMap;
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
}