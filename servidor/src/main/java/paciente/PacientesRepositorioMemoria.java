package paciente;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementación en memoria de D-024: los datos se pierden al reiniciar el servidor. */
public class PacientesRepositorioMemoria implements PacientesRepositorio {
    private final Map<Long, Paciente> pacientes = new LinkedHashMap<>();
    private long siguienteId = 1;

    @Override
    public synchronized Paciente guardar(Paciente paciente) {
        if (paciente.getId() == null) paciente.setId(siguienteId++);
        pacientes.put(paciente.getId(), paciente);
        return paciente;
    }

    @Override
    public synchronized Optional<Paciente> buscarPorId(long id) { return Optional.ofNullable(pacientes.get(id)); }

    @Override
    public synchronized Optional<Paciente> buscarPorDni(String dni) {
        return pacientes.values().stream().filter(p -> p.getDni().equalsIgnoreCase(dni)).findFirst();
    }

    @Override
    public synchronized Optional<Paciente> buscarPorTarjeta(String tarjeta) {
        return pacientes.values().stream()
                .filter(p -> p.getNumeroTarjetaSanitaria() != null && p.getNumeroTarjetaSanitaria().equalsIgnoreCase(tarjeta))
                .findFirst();
    }


        private final List<RegistroModificacion> modificaciones = new ArrayList<>();

    @Override
    public synchronized List<Paciente> buscar(String criterio) {
        String c = criterio.trim().toLowerCase();
        return pacientes.values().stream()
                .filter(Paciente::isActivo)
                .filter(p -> contiene(p.getNombre(), c) || contiene(p.getApellidos(), c) || contiene(p.getDni(), c))
                .toList();
    }

    @Override
    public synchronized void registrarModificacion(RegistroModificacion registro) { modificaciones.add(registro); }

    @Override
    public synchronized List<RegistroModificacion> modificaciones(long pacienteId) {
        return modificaciones.stream().filter(m -> m.pacienteId() == pacienteId).toList();
    }

    private static boolean contiene(String texto, String c) { return texto != null && texto.toLowerCase().contains(c); }
}
