package paciente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Servicio en memoria para probar el cliente hasta que exista el servidor. Aplica D-006 como lo hará el servidor. */
public class PacienteServicioSimulado implements PacienteServicio {
    private final List<Paciente> pacientes = new ArrayList<>();
    private long siguienteId = 1;

    @Override
    public Paciente alta(Paciente paciente) throws PacienteDuplicadoException {
        for (Paciente existente : pacientes) {
            if (existente.getDni().equalsIgnoreCase(paciente.getDni().trim()))
                throw new PacienteDuplicadoException("Ya existe un paciente con el DNI " + paciente.getDni());
            if (existente.getNumeroTarjetaSanitaria().equalsIgnoreCase(paciente.getNumeroTarjetaSanitaria().trim()))
                throw new PacienteDuplicadoException("Ya existe un paciente con la tarjeta sanitaria " + paciente.getNumeroTarjetaSanitaria());
        }
        Paciente registrado = new Paciente();
        registrado.setId(siguienteId++);
        registrado.setNombre(paciente.getNombre().trim());
        registrado.setApellidos(paciente.getApellidos().trim());
        registrado.setDni(paciente.getDni().trim());
        registrado.setFechaNacimiento(paciente.getFechaNacimiento());
        registrado.setTelefono(paciente.getTelefono());
        registrado.setEmail(paciente.getEmail());
        registrado.setDireccion(paciente.getDireccion());
        registrado.setNumeroTarjetaSanitaria(paciente.getNumeroTarjetaSanitaria().trim());
        registrado.setFechaAlta(LocalDate.now());
        pacientes.add(registrado);
        return registrado;
    }
}
