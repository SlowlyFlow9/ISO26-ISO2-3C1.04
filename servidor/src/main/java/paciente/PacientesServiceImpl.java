package paciente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

public class PacientesServiceImpl implements PacientesService {
    // ponytail: solo se comprueba el formato del DNI/NIE, no la letra de control
    private static final Pattern DNI_NIE = Pattern.compile("[0-9]{8}[A-Za-z]|[XYZxyz][0-9]{7}[A-Za-z]");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern TELEFONO = Pattern.compile("\\+?[0-9 ]{9,15}");

    private final PacientesRepositorio repositorio;

    public PacientesServiceImpl(PacientesRepositorio repositorio) { this.repositorio = repositorio; }

    @Override
    public synchronized Paciente crear(Usuario solicitante, Paciente datos)
            throws SinPermisoException, DatosInvalidosException, PacienteDuplicadoException {
        ControlAcceso.exigir(solicitante, Rol.PERSONAL_ADMINISTRATIVO);
        List<String> errores = validar(datos);
        if (!errores.isEmpty()) throw new DatosInvalidosException(errores);

        String dni = datos.getDni().trim();
        String tarjeta = vacio(datos.getNumeroTarjetaSanitaria()) ? null : datos.getNumeroTarjetaSanitaria().trim();
        if (repositorio.buscarPorDni(dni).isPresent())
            throw new PacienteDuplicadoException("Ya existe un paciente con el DNI " + dni);
        if (tarjeta != null && repositorio.buscarPorTarjeta(tarjeta).isPresent())
            throw new PacienteDuplicadoException("Ya existe un paciente con la tarjeta sanitaria " + tarjeta);

        Paciente creado = new Paciente();
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
        return repositorio.guardar(creado);
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
        else if (!TELEFONO.matcher(p.getTelefono().trim()).matches()) errores.add("El teléfono no tiene un formato válido");
        if (!vacio(p.getEmail()) && !EMAIL.matcher(p.getEmail().trim()).matches()) errores.add("El email no es válido");
        return errores;
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }

    @Override
    public List<Paciente> buscar(Usuario solicitante, String criterio) {
        if (vacio(criterio)) return List.of();
        return repositorio.buscar(criterio);
    }

    @Override
    public Paciente consultar(Usuario solicitante, long id) throws PacienteNoEncontradoException {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new PacienteNoEncontradoException("No existe el paciente con id " + id));
    }

    @Override
    public synchronized Paciente modificar(Usuario solicitante, long id, Paciente datos)
            throws SinPermisoException, PacienteNoEncontradoException, PacienteYaInactivoException,
                   DatosInvalidosException, PacienteDuplicadoException {
        ControlAcceso.exigir(solicitante, Rol.PERSONAL_ADMINISTRATIVO);
        Paciente actual = consultar(solicitante, id);
        if (!actual.isActivo()) throw new PacienteYaInactivoException("El paciente " + id + " está dado de baja");
        List<String> errores = validar(datos);
        if (!errores.isEmpty()) throw new DatosInvalidosException(errores);

        String dni = datos.getDni().trim();
        String tarjeta = vacio(datos.getNumeroTarjetaSanitaria()) ? null : datos.getNumeroTarjetaSanitaria().trim();
        // el propio paciente no cuenta como duplicado de sí mismo
        if (repositorio.buscarPorDni(dni).filter(otro -> !otro.getId().equals(id)).isPresent())
            throw new PacienteDuplicadoException("Ya existe un paciente con el DNI " + dni);
        if (tarjeta != null && repositorio.buscarPorTarjeta(tarjeta).filter(otro -> !otro.getId().equals(id)).isPresent())
            throw new PacienteDuplicadoException("Ya existe un paciente con la tarjeta sanitaria " + tarjeta);

        actual.setNombre(datos.getNombre().trim());
        actual.setApellidos(datos.getApellidos().trim());
        actual.setDni(dni);
        actual.setFechaNacimiento(datos.getFechaNacimiento());
        actual.setTelefono(datos.getTelefono().trim());
        actual.setEmail(vacio(datos.getEmail()) ? null : datos.getEmail().trim());
        actual.setDireccion(vacio(datos.getDireccion()) ? null : datos.getDireccion().trim());
        actual.setNumeroTarjetaSanitaria(tarjeta);
        Paciente guardado = repositorio.guardar(actual);
        repositorio.registrarModificacion(new RegistroModificacion(id, solicitante.nombre(), LocalDateTime.now()));
        return guardado;
    }
}
