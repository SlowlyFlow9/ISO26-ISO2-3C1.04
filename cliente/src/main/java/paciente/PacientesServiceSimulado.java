package paciente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Servicio en memoria para probar el cliente sin servidor. Aplica las mismas reglas que el servidor (issue #20, D-006, D-016). */
public class PacientesServiceSimulado implements PacientesService {
    private final List<Paciente> pacientes = new ArrayList<>();
    private long siguienteId = 1;

    @Override
    public Paciente crear(Usuario solicitante, Paciente datos)
            throws SinPermisoException, DatosInvalidosException, PacienteDuplicadoException {
        if (!solicitante.puedeDarDeAltaPacientes())
            throw new SinPermisoException("Solo el Personal administrativo puede dar de alta pacientes");
        List<String> errores = ValidadorPaciente.validar(datos);
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
