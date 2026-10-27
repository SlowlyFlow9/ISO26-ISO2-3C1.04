package paciente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PacientesServiceImpl implements PacientesService {
    // ponytail: solo se comprueba el formato del DNI/NIE, no la letra de control
    private static final Pattern DNI_NIE = Pattern.compile("[0-9]{8}[A-Za-z]|[XYZxyz][0-9]{7}[A-Za-z]");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    // ponytail: almacenamiento en memoria, sustituir por persistencia cuando se decida (no hay decisión aún)
    private final List<Paciente> pacientes = new ArrayList<>();
    private long siguienteId = 1;

    @Override
    public synchronized Paciente crear(Paciente datos) throws DatosInvalidosException, PacienteDuplicadoException {
        List<String> errores = validar(datos);
        if (!errores.isEmpty()) throw new DatosInvalidosException(errores);

        String dni = datos.getDni().trim();
        String tarjeta = vacio(datos.getNumeroTarjetaSanitaria()) ? null : datos.getNumeroTarjetaSanitaria().trim();
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

    private static List<String> validar(Paciente p) {
        List<String> errores = new ArrayList<>();
        if (vacio(p.getNombre())) errores.add("El nombre es obligatorio");
        if (vacio(p.getApellidos())) errores.add("Los apellidos son obligatorios");
        if (vacio(p.getDni())) errores.add("El DNI/NIE es obligatorio");
        else if (!DNI_NIE.matcher(p.getDni().trim()).matches()) errores.add("El DNI/NIE no tiene un formato válido");
        if (p.getFechaNacimiento() == null) errores.add("La fecha de nacimiento es obligatoria");
        else if (p.getFechaNacimiento().isAfter(LocalDate.now())) errores.add("La fecha de nacimiento no puede ser futura");
        if (vacio(p.getTelefono())) errores.add("El teléfono es obligatorio");
        if (!vacio(p.getEmail()) && !EMAIL.matcher(p.getEmail().trim()).matches()) errores.add("El email no es válido");
        return errores;
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }
}
