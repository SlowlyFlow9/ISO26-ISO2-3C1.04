package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CU23: buscar, consultar y modificar en el servicio simulado del cliente. */
class PacientesServiceSimuladoModificarTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesServiceSimulado servicio;

    @BeforeEach
    void preparar() throws Exception {
        servicio = new PacientesServiceSimulado();
        servicio.crear(ADMIN, datos("Ana", "12345678Z", "T1"));
        servicio.crear(ADMIN, datos("Luis", "87654321X", "T2"));
    }

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

    @Test
    void buscarPorNombreApellidosODniSinDistinguirMayusculas() {
        assertEquals(1, servicio.buscar(ADMIN, "LUIS").size());
        assertEquals(2, servicio.buscar(ADMIN, "pérez").size());
        assertEquals(1, servicio.buscar(ADMIN, "8765").size());
        assertEquals(0, servicio.buscar(ADMIN, "zzz").size());
    }

    @Test
    void criterioVacioDevuelveListaVacia() {
        assertTrue(servicio.buscar(ADMIN, "").isEmpty());
        assertTrue(servicio.buscar(ADMIN, null).isEmpty());
    }

    @Test
    void consultarDevuelvePacienteEInexistenteLanzaNoEncontrado() throws Exception {
        assertEquals("12345678Z", servicio.consultar(ADMIN, 1L).getDni());
        assertThrows(PacienteNoEncontradoException.class, () -> servicio.consultar(ADMIN, 99L));
    }

    @Test
    void modificarCambiaLosDatosYLimpiaEspacios() throws Exception {
        Paciente nuevos = datos("  Pedro ", "12345678Z", "T1");
        nuevos.setEmail(" pedro@correo.es ");
        Paciente r = servicio.modificar(ADMIN, 1L, nuevos);
        assertEquals("Pedro", r.getNombre());
        assertEquals("pedro@correo.es", r.getEmail());
        assertEquals("Pedro", servicio.consultar(ADMIN, 1L).getNombre());
    }

    @Test
    void conservarSuPropioDniNoEsDuplicadoPeroElDeOtroSi() throws Exception {
        servicio.modificar(ADMIN, 1L, datos("Ana", "12345678Z", "T1"));
        assertThrows(PacienteDuplicadoException.class,
                () -> servicio.modificar(ADMIN, 2L, datos("Luis", "12345678Z", "T2")));
        assertEquals("87654321X", servicio.consultar(ADMIN, 2L).getDni());
    }

    @Test
    void pacienteInexistenteYDatosInvalidos() {
        assertThrows(PacienteNoEncontradoException.class,
                () -> servicio.modificar(ADMIN, 99L, datos("Ana", "12345678Z", "T1")));
        Paciente sinNombre = datos(" ", "12345678Z", "T1");
        assertThrows(DatosInvalidosException.class, () -> servicio.modificar(ADMIN, 1L, sinNombre));
    }

    @Test
    void pacienteDeBajaNoSePuedeModificar() throws Exception {
        servicio.consultar(ADMIN, 1L).setActivo(false);
        assertThrows(PacienteYaInactivoException.class,
                () -> servicio.modificar(ADMIN, 1L, datos("Ana", "12345678Z", "T1")));
        assertTrue(servicio.buscar(ADMIN, "Ana").isEmpty());
    }

    @Test
    void soloElPersonalAdministrativoPuedeModificar() {
        for (Rol rol : Rol.values()) {
            if (rol == Rol.PERSONAL_ADMINISTRATIVO) continue;
            assertThrows(SinPermisoException.class,
                    () -> servicio.modificar(new Usuario("x", rol), 1L, datos("Ana", "12345678Z", "T1")), rol.name());
        }
    }
}
