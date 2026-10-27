package paciente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PacienteServicioImpl implements PacienteServicio {
    // ponytail: almacenamiento en memoria, sustituir por persistencia cuando se decida (no hay decisión aún)
    private final List<Paciente> pacientes = new ArrayList<>();
    private long siguienteId = 1;

    @Override
    public synchronized Paciente alta(Paciente paciente) throws PacienteDuplicadoException {
        comprobarObligatorios(paciente);
        String dni = paciente.getDni().trim();
        String tarjeta = paciente.getNumeroTarjetaSanitaria().trim();
        for (Paciente existente : pacientes) {
            if (existente.getDni().equalsIgnoreCase(dni))
                throw new PacienteDuplicadoException("Ya existe un paciente con el DNI " + dni);
            if (existente.getNumeroTarjetaSanitaria().equalsIgnoreCase(tarjeta))
                throw new PacienteDuplicadoException("Ya existe un paciente con la tarjeta sanitaria " + tarjeta);
        }
        Paciente registrado = new Paciente();
        registrado.setId(siguienteId++);
        registrado.setNombre(paciente.getNombre().trim());
        registrado.setApellidos(paciente.getApellidos().trim());
        registrado.setDni(dni);
        registrado.setFechaNacimiento(paciente.getFechaNacimiento());
        registrado.setTelefono(paciente.getTelefono());
        registrado.setEmail(paciente.getEmail());
        registrado.setDireccion(paciente.getDireccion());
        registrado.setNumeroTarjetaSanitaria(tarjeta);
        registrado.setFechaAlta(LocalDate.now());
        registrado.setActivo(true);
        pacientes.add(registrado);
        return registrado;
    }

    private static void comprobarObligatorios(Paciente p) {
        if (vacio(p.getNombre()) || vacio(p.getApellidos()) || vacio(p.getDni())
                || p.getFechaNacimiento() == null || vacio(p.getNumeroTarjetaSanitaria()))
            throw new IllegalArgumentException("Faltan campos obligatorios del paciente");
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }
}
