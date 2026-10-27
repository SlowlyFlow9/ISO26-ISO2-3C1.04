package paciente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Servicio en memoria para probar el cliente hasta que exista el servidor. Aplica las reglas de D-006 y de la issue #20 como lo hará el servidor. */
public class PacientesServiceSimulado implements PacientesService {
    private final List<Paciente> pacientes = new ArrayList<>();
    private long siguienteId = 1;

    @Override
    public Paciente crear(Paciente datos) throws DatosInvalidosException, PacienteDuplicadoException {
        List<String> errores = ValidadorPaciente.validar(datos);
        if (datos.getFechaNacimiento() != null && datos.getFechaNacimiento().isAfter(LocalDate.now()))
            errores.add("La fecha de nacimiento no puede ser futura");
        if (!errores.isEmpty()) throw new DatosInvalidosException(errores);

        String dni = datos.getDni().trim();
        String tarjeta = datos.getNumeroTarjetaSanitaria() == null || datos.getNumeroTarjetaSanitaria().isBlank()
                ? null : datos.getNumeroTarjetaSanitaria().trim();
        for (Paciente existente : pacientes) {
            if (existente.getDni().equalsIgnoreCase(dni))
                throw new PacienteDuplicadoException("Ya existe un paciente con el DNI " + dni);
            if (tarjeta != null && tarjeta.equalsIgnoreCase(existente.getNumeroTarjetaSanitaria()))
                throw new PacienteDuplicadoException("Ya existe un paciente con la tarjeta sanitaria " + tarjeta);
        }
        Paciente creado = new Paciente();
        creado.setId(siguienteId++);
        creado.setNombre(datos.getNombre().trim());
        creado.setApellidos(datos.getApellidos().trim());
        creado.setDni(dni);
        creado.setFechaNacimiento(datos.getFechaNacimiento());
        creado.setTelefono(datos.getTelefono().trim());
        creado.setEmail(datos.getEmail());
        creado.setDireccion(datos.getDireccion());
        creado.setNumeroTarjetaSanitaria(tarjeta);
        creado.setFechaAlta(LocalDate.now());
        creado.setActivo(true);
        pacientes.add(creado);
        return creado;
    }
}
