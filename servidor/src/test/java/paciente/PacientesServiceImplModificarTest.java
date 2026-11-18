package paciente;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del CU23: buscar, consultar y modificar paciente. */
class PacientesServiceImplModificarTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesRepositorioMemoria repositorio;
    private PacientesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        repositorio = new PacientesRepositorioMemoria();
        servicio = new PacientesServiceImpl(repositorio);
    }

    @AfterEach
    void tearDown() {
        repositorio = null;
        servicio = null;
    }

    private Paciente datos(String dni, String tarjeta) {
        Paciente p = new Paciente();
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni(dni);
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono("600111222");
        p.setNumeroTarjetaSanitaria(tarjeta);
        return p;
    }

    private Paciente conTelefono(String telefono) {
        Paciente p = datos("12345678Z", "T1");
        p.setTelefono(telefono);
        return p;
    }

    private Paciente altaPaciente(String dni, String tarjeta) throws Exception {
        return servicio.crear(ADMIN, datos(dni, tarjeta));
    }

    private void altaLuisGarcia() throws Exception {
        Paciente luis = datos("87654321X", "T2");
        luis.setNombre("Luis");
        luis.setApellidos("García");
        servicio.crear(ADMIN, luis);
    }

    // ---- Modificar: caso válido ----

    @Test
    @DisplayName("Modificar cambia los datos editables y limpia espacios")
    void modificarCambiaLosDatosEditablesYLimpiaEspacios() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        Paciente nuevos = datos("12345678Z", "T1");
        nuevos.setNombre("  Luis ");
        nuevos.setTelefono("699888777");
        nuevos.setEmail(" luis@correo.es ");
        nuevos.setDireccion("Calle Mayor 1");
        // Ejecutar
        Paciente r = servicio.modificar(ADMIN, 1L, nuevos);
        // Comprobar
        assertAll(
                () -> assertEquals("Luis", r.getNombre()),
                () -> assertEquals("699888777", r.getTelefono()),
                () -> assertEquals("luis@correo.es", r.getEmail()),
                () -> assertEquals("Calle Mayor 1", r.getDireccion()),
                () -> assertEquals("Luis", repositorio.buscarPorId(1L).orElseThrow().getNombre()));
    }

    @Test
    @DisplayName("Modificar no cambia el id, la fecha de alta ni el estado activo")
    void modificarNoCambiaIdFechaAltaNiActivo() throws Exception {
        // Preparar
        Paciente creado = altaPaciente("12345678Z", "T1");
        LocalDate fechaAlta = creado.getFechaAlta();
        Paciente nuevos = datos("12345678Z", "T1");
        nuevos.setId(99L);
        nuevos.setActivo(false);
        nuevos.setFechaAlta(LocalDate.of(2000, 1, 1));
        // Ejecutar
        Paciente r = servicio.modificar(ADMIN, 1L, nuevos);
        // Comprobar
        assertAll(
                () -> assertEquals(1L, r.getId()),
                () -> assertEquals(fechaAlta, r.getFechaAlta()),
                () -> assertTrue(r.isActivo()));
    }

    @Test
    @DisplayName("Conservar su propio DNI y tarjeta no es un duplicado")
    void conservarSuPropioDniYTarjetaNoEsDuplicado() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        // Ejecutar
        Paciente r = servicio.modificar(ADMIN, 1L, datos("12345678Z", "T1"));
        // Comprobar
        assertEquals("12345678Z", r.getDni());
    }

    @Test
    @DisplayName("Cada modificación guarda quién y cuándo")
    void cadaModificacionGuardaQuienYCuando() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        // Ejecutar
        servicio.modificar(ADMIN, 1L, datos("12345678Z", "T1"));
        servicio.modificar(ADMIN, 1L, datos("12345678Z", "T1"));
        // Comprobar
        List<RegistroModificacion> registros = repositorio.modificaciones(1L);
        assertAll(
                () -> assertEquals(2, registros.size()),
                () -> assertEquals("ana", registros.get(0).usuario()),
                () -> assertNotNull(registros.get(0).fecha()));
    }

    // ---- Modificar: duplicados, inexistente y baja ----

    @Test
    @DisplayName("Modificar con el DNI de otro paciente lanza duplicado y no cambia nada")
    void dniDeOtroPacienteLanzaDuplicadoYNoCambiaNada() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        altaPaciente("87654321X", "T2");
        Paciente nuevos = datos(" 12345678z ", "T2");
        // Ejecutar y comprobar
        assertThrows(PacienteDuplicadoException.class, () -> servicio.modificar(ADMIN, 2L, nuevos));
        assertEquals("87654321X", repositorio.buscarPorId(2L).orElseThrow().getDni());
        assertTrue(repositorio.modificaciones(2L).isEmpty());
    }

    @Test
    @DisplayName("Modificar con la tarjeta de otro paciente lanza duplicado")
    void tarjetaDeOtroPacienteLanzaDuplicado() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        altaPaciente("87654321X", "T2");
        Paciente nuevos = datos("87654321X", "T1");
        // Ejecutar y comprobar
        assertThrows(PacienteDuplicadoException.class, () -> servicio.modificar(ADMIN, 2L, nuevos));
    }

    @Test
    @DisplayName("Modificar un paciente inexistente lanza no encontrado")
    void pacienteInexistenteLanzaNoEncontrado() {
        // Preparar
        Paciente nuevos = datos("12345678Z", "T1");
        // Ejecutar y comprobar
        assertThrows(PacienteNoEncontradoException.class, () -> servicio.modificar(ADMIN, 99L, nuevos));
    }

    @Test
    @DisplayName("Un paciente de baja no se puede modificar")
    void pacienteDeBajaNoSePuedeModificar() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        repositorio.buscarPorId(1L).orElseThrow().setActivo(false);
        Paciente nuevos = datos("12345678Z", "T1");
        // Ejecutar y comprobar
        assertThrows(PacienteYaInactivoException.class, () -> servicio.modificar(ADMIN, 1L, nuevos));
        assertTrue(repositorio.modificaciones(1L).isEmpty());
    }

    // ---- Modificar: datos inválidos ----

    @Test
    @DisplayName("Un nombre en blanco lanza datos inválidos")
    void nombreEnBlancoLanzaDatosInvalidos() throws Exception {
        // Preparar
        Paciente sinNombre = datos("12345678Z", "T1");
        sinNombre.setNombre(" ");
        // Ejecutar y comprobar
        assertDatosInvalidos(sinNombre);
    }

    @Test
    @DisplayName("Un DNI con formato incorrecto lanza datos inválidos")
    void dniConFormatoIncorrectoLanzaDatosInvalidos() throws Exception {
        assertDatosInvalidos(datos("123", "T1"));
    }

    @Test
    @DisplayName("Un email sin arroba lanza datos inválidos")
    void emailSinArrobaLanzaDatosInvalidos() throws Exception {
        // Preparar
        Paciente emailMalo = datos("12345678Z", "T1");
        emailMalo.setEmail("sin-arroba");
        // Ejecutar y comprobar
        assertDatosInvalidos(emailMalo);
    }

    @Test
    @DisplayName("Una fecha de nacimiento futura lanza datos inválidos")
    void fechaNacimientoFuturaLanzaDatosInvalidos() throws Exception {
        // Preparar
        Paciente fechaFutura = datos("12345678Z", "T1");
        fechaFutura.setFechaNacimiento(LocalDate.now().plusDays(1));
        // Ejecutar y comprobar
        assertDatosInvalidos(fechaFutura);
    }

    @Test
    @DisplayName("Con datos inválidos el paciente no cambia y no se registra modificación")
    void datosInvalidosNoCambianNadaNiRegistran() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        Paciente sinNombre = datos("12345678Z", "T1");
        sinNombre.setNombre(" ");
        // Ejecutar
        assertThrows(DatosInvalidosException.class, () -> servicio.modificar(ADMIN, 1L, sinNombre));
        // Comprobar
        assertAll(
                () -> assertEquals("Ana", repositorio.buscarPorId(1L).orElseThrow().getNombre()),
                () -> assertTrue(repositorio.modificaciones(1L).isEmpty()));
    }

    private void assertDatosInvalidos(Paciente nuevos) throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        // Ejecutar y comprobar
        assertThrows(DatosInvalidosException.class, () -> servicio.modificar(ADMIN, 1L, nuevos));
    }

    // ---- Modificar: teléfono ----

    @Test
    @DisplayName("Un teléfono con letras es inválido")
    void telefonoConLetrasEsInvalido() throws Exception { assertTelefonoInvalido("abc123"); }

    @Test
    @DisplayName("Un teléfono demasiado corto es inválido")
    void telefonoDemasiadoCortoEsInvalido() throws Exception { assertTelefonoInvalido("123"); }

    @Test
    @DisplayName("Un teléfono con guiones es inválido")
    void telefonoConGuionesEsInvalido() throws Exception { assertTelefonoInvalido("600-111-222"); }

    @Test
    @DisplayName("Un teléfono en blanco es inválido")
    void telefonoEnBlancoEsInvalido() throws Exception { assertTelefonoInvalido(" "); }

    private void assertTelefonoInvalido(String telefono) throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        Paciente nuevos = conTelefono(telefono);
        // Ejecutar y comprobar
        assertThrows(DatosInvalidosException.class, () -> servicio.modificar(ADMIN, 1L, nuevos));
    }

    @Test
    @DisplayName("Un teléfono con prefijo +34 se acepta")
    void telefonoConPrefijoSeAcepta() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        // Ejecutar
        Paciente r = servicio.modificar(ADMIN, 1L, conTelefono("+34 612345678"));
        // Comprobar
        assertEquals("+34 612345678", r.getTelefono());
    }

    // ---- Modificar: permisos ----

    @Test
    @DisplayName("El rol PACIENTE no puede modificar")
    void pacienteNoPuedeModificar() throws Exception { assertSinPermiso(new Usuario("x", Rol.PACIENTE)); }

    @Test
    @DisplayName("El rol MEDICO no puede modificar")
    void medicoNoPuedeModificar() throws Exception { assertSinPermiso(new Usuario("x", Rol.MEDICO)); }

    @Test
    @DisplayName("El rol ADMINISTRADOR no puede modificar")
    void administradorNoPuedeModificar() throws Exception { assertSinPermiso(new Usuario("x", Rol.ADMINISTRADOR)); }

    @Test
    @DisplayName("Un usuario nulo no puede modificar")
    void usuarioNuloNoPuedeModificar() throws Exception { assertSinPermiso(null); }

    private void assertSinPermiso(Usuario usuario) throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        Paciente nuevos = datos("12345678Z", "T1");
        // Ejecutar y comprobar
        assertThrows(SinPermisoException.class, () -> servicio.modificar(usuario, 1L, nuevos));
    }

    @Test
    @DisplayName("Sin permiso se rechaza antes que paciente inexistente o datos inválidos")
    void sinPermisoSeRechazaAntesQueInexistenteOInvalido() {
        // Preparar
        Usuario medico = new Usuario("x", Rol.MEDICO);
        // Ejecutar y comprobar
        assertThrows(SinPermisoException.class, () -> servicio.modificar(medico, 99L, new Paciente()));
    }

    // ---- Buscar ----

    @Test
    @DisplayName("Buscar por nombre no distingue mayúsculas")
    void buscarPorNombreSinDistinguirMayusculas() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        altaLuisGarcia();
        // Ejecutar
        var r = servicio.buscar(ADMIN, "ANA");
        // Comprobar
        assertEquals(1, r.size());
    }

    @Test
    @DisplayName("Buscar por apellidos con tilde")
    void buscarPorApellidos() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        altaLuisGarcia();
        // Ejecutar
        var r = servicio.buscar(ADMIN, "garcía");
        // Comprobar
        assertEquals(1, r.size());
    }

    @Test
    @DisplayName("Buscar por parte del DNI")
    void buscarPorParteDelDni() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        altaLuisGarcia();
        // Ejecutar
        var r = servicio.buscar(ADMIN, "8765");
        // Comprobar
        assertEquals(1, r.size());
    }

    @Test
    @DisplayName("Buscar sin coincidencias devuelve lista vacía")
    void buscarSinCoincidenciasDevuelveVacia() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        // Ejecutar
        var r = servicio.buscar(ADMIN, "zzz");
        // Comprobar
        assertEquals(0, r.size());
    }

    @Test
    @DisplayName("Un criterio vacío devuelve lista vacía")
    void criterioVacioDevuelveListaVacia() throws Exception { assertCriterioSinResultados(""); }

    @Test
    @DisplayName("Un criterio con solo espacios devuelve lista vacía")
    void criterioConSoloEspaciosDevuelveListaVacia() throws Exception { assertCriterioSinResultados("   "); }

    @Test
    @DisplayName("Un criterio nulo devuelve lista vacía")
    void criterioNuloDevuelveListaVacia() throws Exception { assertCriterioSinResultados(null); }

    private void assertCriterioSinResultados(String criterio) throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        // Ejecutar
        var r = servicio.buscar(ADMIN, criterio);
        // Comprobar
        assertTrue(r.isEmpty());
    }

    @Test
    @DisplayName("Buscar no devuelve pacientes de baja")
    void buscarNoDevuelvePacientesDeBaja() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        repositorio.buscarPorId(1L).orElseThrow().setActivo(false);
        // Ejecutar
        var r = servicio.buscar(ADMIN, "ana");
        // Comprobar
        assertTrue(r.isEmpty());
    }

    // ---- Consultar ----

    @Test
    @DisplayName("Consultar devuelve el paciente pedido")
    void consultarDevuelveElPaciente() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        // Ejecutar
        Paciente r = servicio.consultar(ADMIN, 1L);
        // Comprobar
        assertEquals("12345678Z", r.getDni());
    }

    @Test
    @DisplayName("Consultar sí devuelve un paciente de baja")
    void consultarDevuelvePacienteDeBaja() throws Exception {
        // Preparar
        altaPaciente("12345678Z", "T1");
        repositorio.buscarPorId(1L).orElseThrow().setActivo(false);
        // Ejecutar
        Paciente r = servicio.consultar(ADMIN, 1L);
        // Comprobar
        assertEquals("12345678Z", r.getDni());
    }

    @Test
    @DisplayName("Consultar un paciente inexistente lanza no encontrado")
    void consultarInexistenteLanzaNoEncontrado() {
        assertThrows(PacienteNoEncontradoException.class, () -> servicio.consultar(ADMIN, 99L));
    }
}
