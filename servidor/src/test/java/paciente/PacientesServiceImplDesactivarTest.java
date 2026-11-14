package paciente;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorioMemoria;
import solicitud.SolicitudesServiceImpl;
import tipoprueba.RepositorioTiposPruebaMemoria;
import tipoprueba.TipoPrueba;

class PacientesServiceImplDesactivarTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);
    private static final Usuario MEDICO = new Usuario("medico", Rol.MEDICO);

    private PacientesRepositorioMemoria repositorio;
    private PacientesServiceImpl servicio;
    private RepositorioTiposPruebaMemoria tipos;
    private SolicitudesServiceImpl servicioSolicitudes;

    @BeforeEach
    void setUp() {
        repositorio = new PacientesRepositorioMemoria();
        servicio = new PacientesServiceImpl(repositorio);
        tipos = new RepositorioTiposPruebaMemoria();
        servicioSolicitudes = new SolicitudesServiceImpl(repositorio, tipos, new SolicitudesRepositorioMemoria());
    }

    private Paciente altaPaciente() throws Exception {
        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellidos("Pérez");
        paciente.setDni("12345678Z");
        paciente.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        paciente.setTelefono("600111222");
        return servicio.crear(ADMIN, paciente);
    }

    private long altaTipoPrueba() {
        return tipos.guardar(new TipoPrueba(0, "Radiografia", "Prueba diagnostica", 15, false, "", true)).getId();
    }

    private SolicitudPrueba solicitudPara(long pacienteId, long tipoId) {
        return new SolicitudPrueba(0, pacienteId, 1, tipoId, "Dolor", "Exploracion", Prioridad.ORDINARIA,
                LocalDateTime.now(), EstadoSolicitud.PENDIENTE_CITACION);
    }

    // ---- Baja correcta ----

    @Test
    @DisplayName("Dar de baja a un paciente activo lo deja inactivo")
    void desactivarDejaAlPacienteInactivo() throws Exception {
        // Preparar
        Paciente creado = altaPaciente();
        // Ejecutar
        servicio.desactivar(ADMIN, creado.getId());
        // Comprobar
        assertFalse(repositorio.buscarPorId(creado.getId()).orElseThrow().isActivo());
    }

    @Test
    @DisplayName("La baja es lógica: el paciente conserva sus datos")
    void desactivarConservaLosDatos() throws Exception {
        // Preparar
        Paciente creado = altaPaciente();
        // Ejecutar
        servicio.desactivar(ADMIN, creado.getId());
        // Comprobar
        Paciente guardado = repositorio.buscarPorId(creado.getId()).orElseThrow();
        assertAll(
                () -> assertEquals(creado.getDni(), guardado.getDni()),
                () -> assertEquals(creado.getFechaAlta(), guardado.getFechaAlta()));
    }

    @Test
    @DisplayName("Un paciente dado de baja no aparece en la búsqueda")
    void pacienteInactivoNoApareceEnLaBusqueda() throws Exception {
        // Preparar
        Paciente creado = altaPaciente();
        // Ejecutar
        servicio.desactivar(ADMIN, creado.getId());
        // Comprobar
        assertTrue(repositorio.buscar("Ana").isEmpty());
    }

    // ---- Historial y solicitudes ----

    @Test
    @DisplayName("La baja conserva el historial de pruebas del paciente")
    void desactivarConservaElHistorialDePruebas() throws Exception {
        // Preparar
        Paciente paciente = altaPaciente();
        SolicitudPrueba solicitud = servicioSolicitudes.crearSolicitud(MEDICO,
                solicitudPara(paciente.getId(), altaTipoPrueba()));
        // Ejecutar
        servicio.desactivar(ADMIN, paciente.getId());
        // Comprobar
        assertTrue(servicioSolicitudes.consultarSolicitud(solicitud.getId()).isPresent());
    }

    @Test
    @DisplayName("Un paciente dado de baja no puede recibir nuevas solicitudes")
    void pacienteInactivoRechazaNuevasSolicitudes() throws Exception {
        // Preparar
        Paciente paciente = altaPaciente();
        long tipoId = altaTipoPrueba();
        servicio.desactivar(ADMIN, paciente.getId());
        SolicitudPrueba nueva = solicitudPara(paciente.getId(), tipoId);
        // Ejecutar y comprobar
        assertThrows(IllegalArgumentException.class, () -> servicioSolicitudes.crearSolicitud(MEDICO, nueva));
    }

    // ---- Errores ----

    @Test
    @DisplayName("Dar de baja un paciente inexistente lanza no encontrado")
    void pacienteInexistenteLanzaNoEncontrado() {
        // Ejecutar y comprobar
        assertThrows(PacienteNoEncontradoException.class, () -> servicio.desactivar(ADMIN, 99L));
    }

    @Test
    @DisplayName("Dar de baja un paciente ya inactivo lanza ya inactivo")
    void pacienteYaInactivoLanzaExcepcion() throws Exception {
        // Preparar
        Paciente creado = altaPaciente();
        servicio.desactivar(ADMIN, creado.getId());
        // Ejecutar y comprobar
        assertThrows(PacienteYaInactivoException.class, () -> servicio.desactivar(ADMIN, creado.getId()));
    }

    // ---- Permisos ----

    @Test
    @DisplayName("El rol PACIENTE no puede dar de baja")
    void pacienteNoPuedeDarDeBaja() throws Exception { assertSinPermiso(new Usuario("x", Rol.PACIENTE)); }

    @Test
    @DisplayName("El rol MEDICO no puede dar de baja")
    void medicoNoPuedeDarDeBaja() throws Exception { assertSinPermiso(new Usuario("x", Rol.MEDICO)); }

    @Test
    @DisplayName("El rol ADMINISTRADOR no puede dar de baja")
    void administradorNoPuedeDarDeBaja() throws Exception { assertSinPermiso(new Usuario("x", Rol.ADMINISTRADOR)); }

    @Test
    @DisplayName("Un usuario nulo no puede dar de baja")
    void usuarioNuloNoPuedeDarDeBaja() throws Exception { assertSinPermiso(null); }

    private void assertSinPermiso(Usuario usuario) throws Exception {
        // Preparar
        Paciente creado = altaPaciente();
        // Ejecutar
        assertThrows(SinPermisoException.class, () -> servicio.desactivar(usuario, creado.getId()));
        // Comprobar
        assertTrue(repositorio.buscarPorId(creado.getId()).orElseThrow().isActivo());
    }
}
