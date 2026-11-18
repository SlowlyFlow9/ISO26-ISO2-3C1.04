package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PacientesServiceSimuladoDesactivarTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesServiceSimulado servicio;

    @BeforeEach
    void setUp() throws Exception {
        servicio = new PacientesServiceSimulado();
        servicio.crear(ADMIN, paciente());
    }

    private Paciente paciente() {
        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellidos("Pérez");
        paciente.setDni("12345678Z");
        paciente.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        paciente.setTelefono("600111222");
        return paciente;
    }

    @Test
    @DisplayName("Dar de baja a un paciente activo lo deja inactivo")
    void bajaDejaAlPacienteInactivo() throws Exception {
        // Ejecutar
        servicio.desactivar(ADMIN, 1L);
        // Comprobar
        assertFalse(servicio.consultar(ADMIN, 1L).isActivo());
    }

    @Test
    @DisplayName("La baja conserva los datos del paciente")
    void bajaConservaLosDatos() throws Exception {
        // Ejecutar
        servicio.desactivar(ADMIN, 1L);
        // Comprobar
        assertEquals("12345678Z", servicio.consultar(ADMIN, 1L).getDni());
    }

    @Test
    @DisplayName("Un paciente dado de baja no aparece en la búsqueda")
    void pacienteInactivoNoApareceEnLaBusqueda() throws Exception {
        // Ejecutar
        servicio.desactivar(ADMIN, 1L);
        // Comprobar
        assertTrue(servicio.buscar(ADMIN, "Ana").isEmpty());
    }

    @Test
    @DisplayName("Dar de baja un paciente inexistente lanza no encontrado")
    void pacienteInexistenteLanzaNoEncontrado() {
        // Ejecutar y comprobar
        assertThrows(PacienteNoEncontradoException.class, () -> servicio.desactivar(ADMIN, 99L));
    }

    @Test
    @DisplayName("Dar de baja un paciente ya inactivo lanza ya inactivo")
    void pacienteYaDadoDeBajaLanzaYaInactivo() throws Exception {
        // Preparar
        servicio.desactivar(ADMIN, 1L);
        // Ejecutar y comprobar
        assertThrows(PacienteYaInactivoException.class, () -> servicio.desactivar(ADMIN, 1L));
    }

    @Test
    @DisplayName("El rol PACIENTE no puede dar de baja")
    void pacienteNoPuedeDarDeBaja() { assertSinPermiso(new Usuario("usuario", Rol.PACIENTE)); }

    @Test
    @DisplayName("El rol MEDICO no puede dar de baja")
    void medicoNoPuedeDarDeBaja() { assertSinPermiso(new Usuario("usuario", Rol.MEDICO)); }

    @Test
    @DisplayName("El rol ADMINISTRADOR no puede dar de baja")
    void administradorNoPuedeDarDeBaja() { assertSinPermiso(new Usuario("usuario", Rol.ADMINISTRADOR)); }

    @Test
    @DisplayName("Un usuario nulo no puede dar de baja")
    void usuarioNuloNoPuedeDarDeBaja() { assertSinPermiso(null); }

    private void assertSinPermiso(Usuario usuario) {
        // Ejecutar y comprobar
        assertThrows(SinPermisoException.class, () -> servicio.desactivar(usuario, 1L));
    }
}
