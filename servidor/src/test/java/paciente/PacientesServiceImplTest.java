package paciente;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PacientesServiceImplTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesRepositorioMemoria repositorio;
    private PacientesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        repositorio = new PacientesRepositorioMemoria();
        servicio = new PacientesServiceImpl(repositorio);
    }

    private Paciente paciente(String dni, String tarjeta) {
        Paciente p = new Paciente();
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni(dni);
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono("600111222");
        p.setNumeroTarjetaSanitaria(tarjeta);
        return p;
    }

    private Paciente valido() { return paciente("12345678Z", "T1"); }

    // ---- Alta correcta ----

    @Test
    @DisplayName("El alta correcta asigna id, fecha de alta y estado activo")
    void altaCorrectaAsignaIdFechaAltaYActivo() throws Exception {
        // Preparar
        Paciente entrada = valido();
        entrada.setId(99L);
        entrada.setActivo(false);
        // Ejecutar
        Paciente r = servicio.crear(ADMIN, entrada);
        // Comprobar
        assertAll(
                () -> assertEquals(1L, r.getId()),
                () -> assertNotNull(r.getFechaAlta()),
                () -> assertTrue(r.isActivo()));
    }

    @Test
    @DisplayName("El paciente creado queda disponible en el repositorio")
    void elPacienteCreadoQuedaDisponibleEnElRepositorio() throws Exception {
        // Preparar
        Paciente entrada = valido();
        // Ejecutar
        Paciente r = servicio.crear(ADMIN, entrada);
        // Comprobar
        assertEquals("12345678Z", repositorio.buscarPorId(r.getId()).orElseThrow().getDni());
    }

    @Test
    @DisplayName("Los ids se asignan de forma consecutiva")
    void idsConsecutivos() throws Exception {
        // Preparar
        Paciente primero = paciente("12345678Z", "T1");
        Paciente segundo = paciente("87654321X", "T2");
        // Ejecutar
        Paciente r1 = servicio.crear(ADMIN, primero);
        Paciente r2 = servicio.crear(ADMIN, segundo);
        // Comprobar
        assertAll(
                () -> assertEquals(1L, r1.getId()),
                () -> assertEquals(2L, r2.getId()));
    }

    // ---- Duplicados ----

    @Test
    @DisplayName("Un DNI repetido, sin distinguir mayúsculas ni espacios, lanza duplicado")
    void dniRepetidoSinDistinguirMayusculasNiEspacios() throws Exception {
        // Preparar
        servicio.crear(ADMIN, paciente("X1234567L", "T1"));
        Paciente repetido = paciente(" x1234567l ", "T2");
        // Ejecutar y comprobar
        assertThrows(PacienteDuplicadoException.class, () -> servicio.crear(ADMIN, repetido));
    }

    @Test
    @DisplayName("Una tarjeta sanitaria repetida lanza duplicado")
    void tarjetaRepetidaLanzaDuplicado() throws Exception {
        // Preparar
        servicio.crear(ADMIN, valido());
        Paciente repetido = paciente("87654321X", "T1");
        // Ejecutar y comprobar
        assertThrows(PacienteDuplicadoException.class, () -> servicio.crear(ADMIN, repetido));
    }

    @Test
    @DisplayName("Dos pacientes sin tarjeta (nula o en blanco) no se consideran duplicados")
    void variosSinTarjetaNoSeConsideranDuplicados() throws Exception {
        // Preparar
        Paciente sinTarjeta = paciente("12345678Z", null);
        Paciente tarjetaEnBlanco = paciente("87654321X", " ");
        // Ejecutar
        Paciente r1 = servicio.crear(ADMIN, sinTarjeta);
        Paciente r2 = servicio.crear(ADMIN, tarjetaEnBlanco);
        // Comprobar
        assertAll(
                () -> assertEquals(1L, r1.getId()),
                () -> assertEquals(2L, r2.getId()));
    }

    @Test
    @DisplayName("Un alta fallida por duplicado no consume id")
    void altaFallidaNoConsumeId() throws Exception {
        // Preparar
        servicio.crear(ADMIN, valido());
        Paciente duplicado = paciente("12345678Z", "T9");
        Paciente siguiente = paciente("87654321X", "T2");
        // Ejecutar
        assertThrows(PacienteDuplicadoException.class, () -> servicio.crear(ADMIN, duplicado));
        Paciente r = servicio.crear(ADMIN, siguiente);
        // Comprobar
        assertEquals(2L, r.getId());
    }

    // ---- Campos obligatorios ----

    @Test
    @DisplayName("Con todos los campos obligatorios vacíos hay un error por cada uno")
    void camposObligatoriosVaciosDanUnErrorCadaUno() {
        // Preparar
        Paciente vacio = new Paciente();
        // Ejecutar
        DatosInvalidosException e = assertThrows(DatosInvalidosException.class, () -> servicio.crear(ADMIN, vacio));
        // Comprobar
        assertEquals(5, e.getErrores().size());
    }

    @Test
    @DisplayName("Un teléfono en blanco es inválido")
    void telefonoVacioEsInvalido() {
        // Preparar
        Paciente p = valido();
        p.setTelefono(" ");
        // Ejecutar y comprobar
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(ADMIN, p));
    }

    @Test
    @DisplayName("Un nombre en blanco es inválido")
    void nombreEnBlancoEsInvalido() {
        // Preparar
        Paciente p = valido();
        p.setNombre("  ");
        // Ejecutar y comprobar
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(ADMIN, p));
    }

    // ---- DNI ----

    @Test
    @DisplayName("Un DNI con 7 dígitos y letra es inválido")
    void dniConSieteDigitosEsInvalido() {
        assertDniInvalido("1234567Z");
    }

    @Test
    @DisplayName("Un DNI con 9 dígitos y sin letra es inválido")
    void dniSoloNumericoEsInvalido() {
        assertDniInvalido("123456789");
    }

    @Test
    @DisplayName("Un DNI con solo letras es inválido")
    void dniSoloLetrasEsInvalido() {
        assertDniInvalido("ABCDEFGHI");
    }

    @Test
    @DisplayName("Un NIE con 8 dígitos es inválido")
    void nieConOchoDigitosEsInvalido() {
        assertDniInvalido("X12345678");
    }

    private void assertDniInvalido(String dni) {
        // Preparar
        Paciente p = paciente(dni, "T1");
        // Ejecutar y comprobar
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(ADMIN, p));
    }

    // ---- Email ----

    @Test
    @DisplayName("Un email sin arroba es inválido")
    void emailSinArrobaEsInvalido() {
        // Preparar
        Paciente p = valido();
        p.setEmail("sin-arroba");
        // Ejecutar y comprobar
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(ADMIN, p));
    }

    @Test
    @DisplayName("Un email con formato correcto es válido")
    void emailCorrectoEsValido() throws Exception {
        // Preparar
        Paciente p = valido();
        p.setEmail("ana@correo.es");
        // Ejecutar
        Paciente r = servicio.crear(ADMIN, p);
        // Comprobar
        assertEquals("ana@correo.es", r.getEmail());
    }

    @Test
    @DisplayName("Un email vacío es válido porque es opcional")
    void emailVacioEsValido() throws Exception {
        // Preparar
        Paciente p = valido();
        p.setEmail("");
        // Ejecutar
        Paciente r = servicio.crear(ADMIN, p);
        // Comprobar
        assertNotNull(r.getId());
    }

    // ---- Fecha de nacimiento ----

    @Test
    @DisplayName("Una fecha de nacimiento futura (mañana) es inválida")
    void fechaNacimientoFuturaEsInvalida() {
        // Preparar
        Paciente p = valido();
        p.setFechaNacimiento(LocalDate.now().plusDays(1));
        // Ejecutar y comprobar
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(ADMIN, p));
    }

    @Test
    @DisplayName("Una fecha de nacimiento de hoy es válida (valor límite)")
    void fechaNacimientoDeHoyEsValida() throws Exception {
        // Preparar
        Paciente p = valido();
        p.setFechaNacimiento(LocalDate.now());
        // Ejecutar
        Paciente r = servicio.crear(ADMIN, p);
        // Comprobar
        assertEquals(LocalDate.now(), r.getFechaNacimiento());
    }

    @Test
    @DisplayName("Una fecha de nacimiento de ayer es válida (valor límite adyacente)")
    void fechaNacimientoDeAyerEsValida() throws Exception {
        // Preparar
        Paciente p = valido();
        p.setFechaNacimiento(LocalDate.now().minusDays(1));
        // Ejecutar
        Paciente r = servicio.crear(ADMIN, p);
        // Comprobar
        assertEquals(LocalDate.now().minusDays(1), r.getFechaNacimiento());
    }

    // ---- Permisos ----

    @Test
    @DisplayName("El rol PACIENTE no puede dar de alta")
    void pacienteNoPuedeDarDeAlta() {
        assertSinPermiso(new Usuario("x", Rol.PACIENTE));
    }

    @Test
    @DisplayName("El rol MEDICO no puede dar de alta")
    void medicoNoPuedeDarDeAlta() {
        assertSinPermiso(new Usuario("x", Rol.MEDICO));
    }

    @Test
    @DisplayName("El rol ADMINISTRADOR no puede dar de alta")
    void administradorNoPuedeDarDeAlta() {
        assertSinPermiso(new Usuario("x", Rol.ADMINISTRADOR));
    }

    @Test
    @DisplayName("Un usuario nulo no puede dar de alta")
    void usuarioNuloNoPuedeDarDeAlta() {
        assertSinPermiso(null);
    }

    private void assertSinPermiso(Usuario usuario) {
        // Preparar
        Paciente p = valido();
        // Ejecutar y comprobar
        assertThrows(SinPermisoException.class, () -> servicio.crear(usuario, p));
    }

    @Test
    @DisplayName("Se rechaza por falta de permiso antes que por datos inválidos")
    void sinPermisoSeRechazaAntesQueLosDatosInvalidos() {
        // Preparar
        Usuario medico = new Usuario("x", Rol.MEDICO);
        Paciente vacio = new Paciente();
        // Ejecutar y comprobar
        assertThrows(SinPermisoException.class, () -> servicio.crear(medico, vacio));
    }
}
