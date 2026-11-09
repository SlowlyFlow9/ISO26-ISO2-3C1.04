package prueba;

import informe.Usuario;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PruebasService {
    private final PruebasRepositorio repositorio;

    public PruebasService(PruebasRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public void marcarComoRealizada(Usuario solicitante, String idPrueba) {
        if (!"Médico".equals(solicitante.getRol())) {
            throw new RuntimeException("Solo los médicos pueden modificar el estado de las pruebas");
        }
        if (idPrueba == null || idPrueba.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de la prueba es obligatorio");
        }
        PruebaMedica prueba = repositorio.obtenerPorId(idPrueba);
        if (prueba == null) {
            throw new IllegalArgumentException("No existe ninguna prueba con ese ID");
        }
        prueba.setEstado("REALIZADA");
    }

    public void registrarIncidencia(Usuario solicitante, String idPrueba, String descripcion) {
        if (!"Médico".equals(solicitante.getRol())) {
            throw new RuntimeException("Solo los médicos pueden registrar incidencias");
        }
        if (idPrueba == null || idPrueba.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de la prueba es obligatorio");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción de la incidencia no puede estar vacía");
        }

        PruebaMedica prueba = repositorio.obtenerPorId(idPrueba);
        if (prueba == null) {
            throw new IllegalArgumentException("No existe ninguna prueba con ese ID");
        }

        prueba.setIncidencia(descripcion);
    }

    // Nuevo método para CU21
    public Map<String, Integer> consultarEstadisticas(Usuario solicitante) {
        if (!"Médico".equals(solicitante.getRol())) {
            throw new RuntimeException("Solo los médicos pueden consultar estadísticas");
        }

        List<PruebaMedica> todas = repositorio.obtenerTodas();
        Map<String, Integer> estadisticas = new HashMap<>();
        
        estadisticas.put("PENDIENTE", 0);
        estadisticas.put("REALIZADA", 0);

        for (PruebaMedica p : todas) {
            String estado = p.getEstado();
            estadisticas.put(estado, estadisticas.getOrDefault(estado, 0) + 1);
        }

        return estadisticas;
    }
}