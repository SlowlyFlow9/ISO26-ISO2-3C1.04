package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Paciente;
import paciente.PacientesRepositorioMemoria;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import tipoprueba.RepositorioTiposPruebaMemoria;
import tipoprueba.TipoPrueba;

/** CU12: reglas de negocio de la prescripción de pruebas de seguimiento (D-030). */
class SolicitudesServiceSeguimientoTest {
    private static final Usuario MEDICO = new Usuario("medico", Rol.MEDICO);

    private PacientesRepositorioMemoria pacientes;
    private RepositorioTiposPruebaMemoria tipos;
    private SolicitudesRepositorioMemoria solicitudes;
    private SolicitudesServiceImpl servicio;
    private long pacienteId;
    private long tipoId;

    @BeforeEach
    void setUp() {
        pacientes = new PacientesRepositorioMemoria();
        tipos = new RepositorioTiposPruebaMemoria();
        solicitudes = new SolicitudesRepositorioMemoria();
        servicio = new SolicitudesServiceImpl(pacientes, tipos, solicitudes);
        pacienteId = pacientes.guardar(new Paciente()).getId();
        tipoId = tipos.guardar(new TipoPrueba(0, "Radiografia", "Prueba diagnostica", 15, false, "", true)).getId();
    }

    private SolicitudPrueba seguimiento(long paciente, long tipo, Integer intervalo, UnidadPeriodicidad unidad,
            Integer repeticiones, Long origen) {
        return new SolicitudPrueba(0, paciente, 2, tipo, "Dolor de rodilla", "Control de evolución",
                Prioridad.PREFERENTE, null, null, intervalo, unidad, repeticiones, origen);
    }

    private SolicitudPrueba valido() {
        return seguimiento(pacienteId, tipoId, 7, UnidadPeriodicidad.SEMANAS, 3, null);
    }

    private SolicitudPrueba conPeriodicidad(Integer intervalo, UnidadPeriodicidad unidad, Integer repeticiones) {
        return seguimiento(pacienteId, tipoId, intervalo, unidad, repeticiones, null);
    }

    private void assertDatosInvalidos(SolicitudPrueba datos) {
        // Ejecutar y comprobar
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitud(MEDICO, datos));
    }

    // ---- Registro correcto ----

    @Test
    @DisplayName("Un seguimiento válido se registra con su periodicidad")
    void seguimientoValidoSeRegistraConSuPeriodicidad() throws Exception {
        // Preparar
        SolicitudPrueba datos = valido();
        // Ejecutar
        SolicitudPrueba creada = servicio.crearSolicitud(MEDICO, datos);
        // Comprobar
        assertAll(
                () -> assertTrue(creada.getId() > 0),
                () -> assertTrue(creada.esSeguimiento()),
                () -> assertEquals(7, creada.getIntervalo()),
                () -> assertEquals(UnidadPeriodicidad.SEMANAS, creada.getUnidad()),
                () -> assertEquals(3, creada.getNumeroRepeticiones()));
    }

    @Test
    @DisplayName("Un seguimiento se registra con el estado PENDIENTE_CITACION")
    void seguimientoSeRegistraPendienteDeCitacion() throws Exception {
        // Preparar
        SolicitudPrueba datos = valido();
        // Ejecutar
        SolicitudPrueba creada = servicio.crearSolicitud(MEDICO, datos);
        // Comprobar
        assertEquals(EstadoSolicitud.PENDIENTE_CITACION, creada.getEstado());
    }

    @Test
    @DisplayName("consultarSolicitud devuelve el seguimiento con su periodicidad")
    void consultarDevuelveElSeguimiento() throws Exception {
        // Preparar
        SolicitudPrueba creada = servicio.crearSolicitud(MEDICO, valido());
        // Ejecutar
        SolicitudPrueba consultada = servicio.consultarSolicitud(creada.getId()).orElseThrow();
        // Comprobar
        assertAll(
                () -> assertTrue(consultada.esSeguimiento()),
                () -> assertEquals(7, consultada.getIntervalo()),
                () -> assertEquals(UnidadPeriodicidad.SEMANAS, consultada.getUnidad()),
                () -> assertEquals(3, consultada.getNumeroRepeticiones()));
    }

    @Test
    @DisplayName("Una solicitud sin periodicidad se registra como en CU11 y no es un seguimiento")
    void solicitudSinPeriodicidadNoEsSeguimiento() throws Exception {
        // Preparar
        SolicitudPrueba datos = conPeriodicidad(null, null, null);
        // Ejecutar
        SolicitudPrueba creada = servicio.crearSolicitud(MEDICO, datos);
        // Comprobar
        assertFalse(creada.esSeguimiento());
    }

    @Test
    @DisplayName("Un intervalo de 1 día es válido (valor límite)")
    void intervaloUnoEsValido() throws Exception {
        // Preparar
        SolicitudPrueba datos = conPeriodicidad(1, UnidadPeriodicidad.DIAS, 1);
        // Ejecutar
        SolicitudPrueba creada = servicio.crearSolicitud(MEDICO, datos);
        // Comprobar
        assertAll(
                () -> assertEquals(1, creada.getIntervalo()),
                () -> assertEquals(UnidadPeriodicidad.DIAS, creada.getUnidad()));
    }

    @Test
    @DisplayName("La unidad MESES es válida")
    void unidadMesesEsValida() throws Exception {
        // Preparar
        SolicitudPrueba datos = conPeriodicidad(6, UnidadPeriodicidad.MESES, 2);
        // Ejecutar
        SolicitudPrueba creada = servicio.crearSolicitud(MEDICO, datos);
        // Comprobar
        assertEquals(UnidadPeriodicidad.MESES, creada.getUnidad());
    }

    // ---- Periodicidad inválida ----

    @Test
    @DisplayName("Un intervalo de 0 se rechaza (valor límite)")
    void intervaloCeroSeRechaza() { assertDatosInvalidos(conPeriodicidad(0, UnidadPeriodicidad.DIAS, 3)); }

    @Test
    @DisplayName("Un intervalo negativo se rechaza")
    void intervaloNegativoSeRechaza() { assertDatosInvalidos(conPeriodicidad(-1, UnidadPeriodicidad.DIAS, 3)); }

    @Test
    @DisplayName("Un número de repeticiones de 0 se rechaza (valor límite)")
    void repeticionesCeroSeRechaza() { assertDatosInvalidos(conPeriodicidad(7, UnidadPeriodicidad.DIAS, 0)); }

    @Test
    @DisplayName("Un número de repeticiones negativo se rechaza")
    void repeticionesNegativasSeRechazan() { assertDatosInvalidos(conPeriodicidad(7, UnidadPeriodicidad.DIAS, -1)); }

    @Test
    @DisplayName("Una periodicidad sin intervalo se rechaza")
    void faltaIntervaloSeRechaza() { assertDatosInvalidos(conPeriodicidad(null, UnidadPeriodicidad.DIAS, 3)); }

    @Test
    @DisplayName("Una periodicidad sin unidad se rechaza")
    void faltaUnidadSeRechaza() { assertDatosInvalidos(conPeriodicidad(7, null, 3)); }

    @Test
    @DisplayName("Una periodicidad sin repeticiones se rechaza")
    void faltanRepeticionesSeRechaza() { assertDatosInvalidos(conPeriodicidad(7, UnidadPeriodicidad.DIAS, null)); }

    @Test
    @DisplayName("Una periodicidad inválida no registra la solicitud")
    void periodicidadInvalidaNoRegistraLaSolicitud() {
        // Preparar
        SolicitudPrueba datos = conPeriodicidad(0, UnidadPeriodicidad.DIAS, 3);
        // Ejecutar
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitud(MEDICO, datos));
        // Comprobar
        assertTrue(solicitudes.buscarPorId(1).isEmpty());
    }

    // ---- Prescripción original ----

    @Test
    @DisplayName("Un seguimiento con una prescripción original del mismo paciente es válido")
    void origenDelMismoPacienteEsValido() throws Exception {
        // Preparar
        SolicitudPrueba original = servicio.crearSolicitud(MEDICO, valido());
        SolicitudPrueba datos = seguimiento(pacienteId, tipoId, 1, UnidadPeriodicidad.MESES, 2, original.getId());
        // Ejecutar
        SolicitudPrueba creada = servicio.crearSolicitud(MEDICO, datos);
        // Comprobar
        assertEquals(original.getId(), creada.getSolicitudOrigenId());
    }

    @Test
    @DisplayName("Una prescripción original inexistente se rechaza")
    void origenInexistenteSeRechaza() {
        // Preparar
        SolicitudPrueba datos = seguimiento(pacienteId, tipoId, 7, UnidadPeriodicidad.DIAS, 3, 99L);
        // Ejecutar y comprobar
        assertThrows(NoSuchElementException.class, () -> servicio.crearSolicitud(MEDICO, datos));
    }

    @Test
    @DisplayName("Una prescripción original de otro paciente se rechaza")
    void origenDeOtroPacienteSeRechaza() throws Exception {
        // Preparar
        SolicitudPrueba original = servicio.crearSolicitud(MEDICO, valido());
        long otroPaciente = pacientes.guardar(new Paciente()).getId();
        SolicitudPrueba datos = seguimiento(otroPaciente, tipoId, 7, UnidadPeriodicidad.DIAS, 3, original.getId());
        // Ejecutar y comprobar
        assertDatosInvalidos(datos);
    }

    @Test
    @DisplayName("Una prescripción original de 0 se rechaza (valor límite)")
    void origenCeroSeRechaza() {
        assertDatosInvalidos(seguimiento(pacienteId, tipoId, 7, UnidadPeriodicidad.DIAS, 3, 0L));
    }

    // ---- Paciente y tipo de prueba ----

    @Test
    @DisplayName("Un seguimiento de un paciente inexistente se rechaza")
    void pacienteInexistenteSeRechaza() {
        // Preparar
        SolicitudPrueba datos = seguimiento(99, tipoId, 7, UnidadPeriodicidad.DIAS, 3, null);
        // Ejecutar y comprobar
        assertThrows(NoSuchElementException.class, () -> servicio.crearSolicitud(MEDICO, datos));
    }

    @Test
    @DisplayName("Un seguimiento de un paciente dado de baja se rechaza")
    void pacienteInactivoSeRechaza() {
        // Preparar
        Paciente baja = new Paciente();
        baja.setActivo(false);
        long inactivo = pacientes.guardar(baja).getId();
        // Ejecutar y comprobar
        assertDatosInvalidos(seguimiento(inactivo, tipoId, 7, UnidadPeriodicidad.DIAS, 3, null));
    }

    @Test
    @DisplayName("Un seguimiento con un tipo de prueba inexistente se rechaza")
    void tipoInexistenteSeRechaza() {
        // Preparar
        SolicitudPrueba datos = seguimiento(pacienteId, 99, 7, UnidadPeriodicidad.DIAS, 3, null);
        // Ejecutar y comprobar
        assertThrows(NoSuchElementException.class, () -> servicio.crearSolicitud(MEDICO, datos));
    }

    @Test
    @DisplayName("Un seguimiento con un tipo de prueba dado de baja se rechaza")
    void tipoInactivoSeRechaza() {
        // Preparar
        long inactivo = tipos.guardar(new TipoPrueba(0, "Antigua", "Ya no se usa", 15, false, "", false)).getId();
        // Ejecutar y comprobar
        assertDatosInvalidos(seguimiento(pacienteId, inactivo, 7, UnidadPeriodicidad.DIAS, 3, null));
    }

    // ---- Permisos ----

    @Test
    @DisplayName("El rol PACIENTE no puede prescribir un seguimiento")
    void pacienteNoPuedePrescribir() { assertSinPermiso(new Usuario("x", Rol.PACIENTE)); }

    @Test
    @DisplayName("El rol PERSONAL_ADMINISTRATIVO no puede prescribir un seguimiento")
    void personalAdministrativoNoPuedePrescribir() {
        assertSinPermiso(new Usuario("x", Rol.PERSONAL_ADMINISTRATIVO));
    }

    @Test
    @DisplayName("El rol ADMINISTRADOR no puede prescribir un seguimiento")
    void administradorNoPuedePrescribir() { assertSinPermiso(new Usuario("x", Rol.ADMINISTRADOR)); }

    @Test
    @DisplayName("Un usuario nulo no puede prescribir un seguimiento")
    void usuarioNuloNoPuedePrescribir() { assertSinPermiso(null); }

    private void assertSinPermiso(Usuario usuario) {
        // Preparar
        SolicitudPrueba datos = valido();
        // Ejecutar
        assertThrows(SinPermisoException.class, () -> servicio.crearSolicitud(usuario, datos));
        // Comprobar
        assertTrue(solicitudes.buscarPorId(1).isEmpty());
    }
}
