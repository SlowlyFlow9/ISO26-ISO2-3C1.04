package paciente;

import java.util.LinkedHashMap;
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
}
