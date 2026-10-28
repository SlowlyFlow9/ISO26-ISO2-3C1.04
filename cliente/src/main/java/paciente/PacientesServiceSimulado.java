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

    @Override
    public List<Paciente> buscar(Usuario solicitante, String criterio) {
        List<Paciente> encontrados = new ArrayList<>();
        if (criterio == null || criterio.isBlank()) return encontrados;
        String c = criterio.trim().toLowerCase();
        for (Paciente p : pacientes) {
            if (p.isActivo() && (contiene(p.getNombre(), c) || contiene(p.getApellidos(), c) || contiene(p.getDni(), c)))
                encontrados.add(p);
        }
        return encontrados;
    }

    @Override
    public Paciente consultar(Usuario solicitante, long id) throws PacienteNoEncontradoException {
        for (Paciente p : pacientes) {
            if (p.getId() == id) return p;
        }
        throw new PacienteNoEncontradoException("No existe el paciente con id " + id);
    }

    @Override
    public Paciente modificar(Usuario solicitante, long id, Paciente datos)
            throws SinPermisoException, PacienteNoEncontradoException, PacienteYaInactivoException,
                   DatosInvalidosException, PacienteDuplicadoException {
        if (!solicitante.puedeDarDeAltaPacientes())
            throw new SinPermisoException("Solo el Personal administrativo puede modificar pacientes");
        Paciente actual = consultar(solicitante, id);
        if (!actual.isActivo()) throw new PacienteYaInactivoException("El paciente " + id + " está dado de baja");
        List<String> errores = ValidadorPaciente.validar(datos);
        if (!errores.isEmpty()) throw new DatosInvalidosException(errores);

        String dni = datos.getDni().trim();
        String tarjeta = datos.getNumeroTarjetaSanitaria() == null || datos.getNumeroTarjetaSanitaria().isBlank()
                ? null : datos.getNumeroTarjetaSanitaria().trim();
        for (Paciente otro : pacientes) {
            if (otro.getId() == id) continue;
            if (otro.getDni().equalsIgnoreCase(dni))
                throw new PacienteDuplicadoException("Ya existe un paciente con el DNI " + dni);
            if (tarjeta != null && tarjeta.equalsIgnoreCase(otro.getNumeroTarjetaSanitaria()))
                throw new PacienteDuplicadoException("Ya existe un paciente con la tarjeta sanitaria " + tarjeta);
        }
        actual.setNombre(datos.getNombre().trim());
        actual.setApellidos(datos.getApellidos().trim());
        actual.setDni(dni);
        actual.setFechaNacimiento(datos.getFechaNacimiento());
        actual.setTelefono(datos.getTelefono().trim());
        actual.setEmail(datos.getEmail() == null || datos.getEmail().isBlank() ? null : datos.getEmail().trim());
        actual.setDireccion(datos.getDireccion() == null || datos.getDireccion().isBlank() ? null : datos.getDireccion().trim());
        actual.setNumeroTarjetaSanitaria(tarjeta);
        return actual;
    }

    private static boolean contiene(String texto, String c) { return texto != null && texto.toLowerCase().contains(c); }
}
