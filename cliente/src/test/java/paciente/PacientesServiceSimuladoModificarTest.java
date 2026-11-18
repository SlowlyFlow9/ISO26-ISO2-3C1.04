package paciente;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU23: buscar, consultar y modificar en el servicio simulado del cliente. */
class PacientesServiceSimuladoModificarTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesServiceSimulado servicio;

    @BeforeEach
    void setUp() throws Exception {
        servicio = new PacientesServiceSimulado();
        servicio.crear(ADMIN, datos("Ana", "12345678Z", "T1"));
        servicio.crear(ADMIN, datos("Luis", "87654321X", "T2"));
    }

    @AfterEach
    void tearDown() { servicio = null; }

    private Paciente datos(String nombre, String dni, String tarjeta) {
        Paciente p = new Paciente();
        p.setNombre(nombre);
        p.setApellidos("Pérez");
        p.setDni(dni);
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono("600111222");
        p.setNumeroTarjetaSanitaria(tarjeta);
        return p;
    }

    // ---- Buscar ----

    @Test
    @DisplayName("Buscar por nombre no distingue mayúsculas")
    void buscarPorNombreSinDistinguirMayusculas() {
        // Ejecutar
        var r = servicio.buscar(ADMIN, "LUIS");
        // Comprobar
        assertEquals(1, r.size());
    }

    @Test
    @DisplayName("Buscar por apellidos devuelve todos los que coinciden")
    void buscarPorApellidosDevuelveTodos() {
        // Ejecutar
        var r = servicio.buscar(ADMIN, "pérez");
        // Comprobar
        assertEquals(2, r.size());
    }

    @Test
    @DisplayName("Buscar por parte del DNI")
    void buscarPorParteDelDni() {
        // Ejecutar
        var r = servicio.buscar(ADMIN, "8765");
        // Comprobar
        assertEquals(1, r.size());
    }

    @Test
    @DisplayName("Buscar sin coincidencias devuelve lista vacía")
    void buscarSinCoincidenciasDevuelveVacia() {
        // Ejecutar
        var r = servicio.buscar(ADMIN, "zzz");
        // Comprobar
        assertEquals(0, r.size());
    }

    @Test
    @DisplayName("Un criterio vacío devuelve lista vacía")
    void criterioVacioDevuelveListaVacia() { assertCriterioSinResultados(""); }

    @Test
    @DisplayName("Un criterio nulo devuelve lista vacía")
    void criterioNuloDevuelveListaVacia() { assertCriterioSinResultados(null); }

    private void assertCriterioSinResultados(String criterio) {
        // Ejecutar
        var r = servicio.buscar(ADMIN, criterio);
        // Comprobar
        assertTrue(r.isEmpty());
    }

    // ---- Consultar ----

    @Test
    @DisplayName("Consultar devuelve el paciente pedido")
    void consultarDevuelveElPaciente() throws Exception {
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

    // ---- Modificar ----

    @Test
    @DisplayName("Modificar cambia los datos y limpia espacios")
    void modificarCambiaLosDatosYLimpiaEspacios() throws Exception {
        // Preparar
        Paciente nuevos = datos("  Pedro ", "12345678Z", "T1");
        nuevos.setEmail(" pedro@correo.es ");
        // Ejecutar
        Paciente r = servicio.modificar(ADMIN, 1L, nuevos);
        // Comprobar
        assertAll(
                () -> assertEquals("Pedro", r.getNombre()),
                () -> assertEquals("pedro@correo.es", r.getEmail()),
                () -> assertEquals("Pedro", servicio.consultar(ADMIN, 1L).getNombre()));
    }

    @Test
    @DisplayName("Conservar su propio DNI no es un duplicado")
    void conservarSuPropioDniNoEsDuplicado() throws Exception {
        // Preparar
        Paciente nuevos = datos("Ana", "12345678Z", "T1");
        // Ejecutar
        Paciente r = servicio.modificar(ADMIN, 1L, nuevos);
        // Comprobar
        assertEquals("12345678Z", r.getDni());
    }

    @Test
    @DisplayName("Modificar con el DNI de otro paciente lanza duplicado y no cambia nada")
    void dniDeOtroLanzaDuplicadoYNoCambiaNada() throws Exception {
        // Preparar
        Paciente nuevos = datos("Luis", "12345678Z", "T2");
        // Ejecutar y comprobar
        assertThrows(PacienteDuplicadoException.class, () -> servicio.modificar(ADMIN, 2L, nuevos));
        assertEquals("87654321X", servicio.consultar(ADMIN, 2L).getDni());
    }

    @Test
    @DisplayName("Modificar con la tarjeta de otro paciente lanza duplicado")
    void tarjetaDeOtroLanzaDuplicado() {
        // Preparar
        Paciente nuevos = datos("Luis", "87654321X", "T1");
        // Ejecutar y comprobar
        assertThrows(PacienteDuplicadoException.class, () -> servicio.modificar(ADMIN, 2L, nuevos));
    }

    @Test
    @DisplayName("Modificar un paciente inexistente lanza no encontrado")
    void modificarInexistenteLanzaNoEncontrado() {
        // Preparar
        Paciente nuevos = datos("Ana", "12345678Z", "T1");
        // Ejecutar y comprobar
        assertThrows(PacienteNoEncontradoException.class, () -> servicio.modificar(ADMIN, 99L, nuevos));
    }

    @Test
    @DisplayName("Un nombre en blanco lanza datos inválidos")
    void nombreEnBlancoLanzaDatosInvalidos() {
        // Preparar
        Paciente sinNombre = datos(" ", "12345678Z", "T1");
        // Ejecutar y comprobar
        assertThrows(DatosInvalidosException.class, () -> servicio.modificar(ADMIN, 1L, sinNombre));
    }

    @Test
    @DisplayName("Un paciente de baja no se puede modificar")
    void pacienteDeBajaNoSePuedeModificar() throws Exception {
        // Preparar
        servicio.consultar(ADMIN, 1L).setActivo(false);
        Paciente nuevos = datos("Ana", "12345678Z", "T1");
        // Ejecutar y comprobar
        assertThrows(PacienteYaInactivoException.class, () -> servicio.modificar(ADMIN, 1L, nuevos));
    }

    @Test
    @DisplayName("Un paciente de baja no sale en la búsqueda")
    void pacienteDeBajaNoSaleEnLaBusqueda() throws Exception {
        // Preparar
        servicio.consultar(ADMIN, 1L).setActivo(false);
        // Ejecutar
        var r = servicio.buscar(ADMIN, "Ana");
        // Comprobar
        assertTrue(r.isEmpty());
    }

    // ---- Permisos ----

    @Test
    @DisplayName("El rol PACIENTE no puede modificar")
    void pacienteNoPuedeModificar() { assertSinPermiso(Rol.PACIENTE); }

    @Test
    @DisplayName("El rol MEDICO no puede modificar")
    void medicoNoPuedeModificar() { assertSinPermiso(Rol.MEDICO); }

    @Test
    @DisplayName("El rol ADMINISTRADOR no puede modificar")
    void administradorNoPuedeModificar() { assertSinPermiso(Rol.ADMINISTRADOR); }

    private void assertSinPermiso(Rol rol) {
        // Preparar
        Usuario usuario = new Usuario("x", rol);
        Paciente nuevos = datos("Ana", "12345678Z", "T1");
        // Ejecutar y comprobar
        assertThrows(SinPermisoException.class, () -> servicio.modificar(usuario, 1L, nuevos));
    }
}
