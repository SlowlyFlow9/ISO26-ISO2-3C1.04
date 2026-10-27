package paciente;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del CU23: buscar, consultar y modificar paciente. */
class PacientesServiceImplModificarTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesRepositorioMemoria repositorio;
    private PacientesServiceImpl servicio;

    @BeforeEach
    void preparar() {
        repositorio = new PacientesRepositorioMemoria();
        servicio = new PacientesServiceImpl(repositorio);
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

    private Paciente altaPaciente(String dni, String tarjeta) throws Exception {
        return servicio.crear(ADMIN, datos(dni, tarjeta));
    }

    // ---------- modificar ----------

    @Test
    void modificarCambiaLosDatosEditablesYLimpiaEspacios() throws Exception {
        altaPaciente("12345678Z", "T1");
        Paciente nuevos = datos("12345678Z", "T1");
        nuevos.setNombre("  Luis ");
        nuevos.setTelefono("699888777");
        nuevos.setEmail(" luis@correo.es ");
        nuevos.setDireccion("Calle Mayor 1");
        Paciente r = servicio.modificar(ADMIN, 1L, nuevos);
        assertEquals("Luis", r.getNombre());
        assertEquals("699888777", r.getTelefono());
        assertEquals("luis@correo.es", r.getEmail());
        assertEquals("Calle Mayor 1", r.getDireccion());
        assertEquals("Luis", repositorio.buscarPorId(1L).orElseThrow().getNombre());
    }

    @Test
    void modificarNoCambiaIdFechaAltaNiActivo() throws Exception {
        Paciente creado = altaPaciente("12345678Z", "T1");
        LocalDate fechaAlta = creado.getFechaAlta();
        Paciente nuevos = datos("12345678Z", "T1");
        nuevos.setId(99L);
        nuevos.setActivo(false);
        nuevos.setFechaAlta(LocalDate.of(2000, 1, 1));
        Paciente r = servicio.modificar(ADMIN, 1L, nuevos);
        assertEquals(1L, r.getId());
        assertEquals(fechaAlta, r.getFechaAlta());
        assertTrue(r.isActivo());
    }

    @Test
    void conservarSuPropioDniYTarjetaNoEsDuplicado() throws Exception {
        altaPaciente("12345678Z", "T1");
        Paciente r = servicio.modificar(ADMIN, 1L, datos("12345678Z", "T1"));
        assertEquals("12345678Z", r.getDni());
    }

    @Test
    void dniDeOtroPacienteLanzaDuplicadoYNoCambiaNada() throws Exception {
        altaPaciente("12345678Z", "T1");
        altaPaciente("87654321X", "T2");
        Paciente nuevos = datos(" 12345678z ", "T2");
        assertThrows(PacienteDuplicadoException.class, () -> servicio.modificar(ADMIN, 2L, nuevos));
        assertEquals("87654321X", repositorio.buscarPorId(2L).orElseThrow().getDni());
        assertTrue(repositorio.modificaciones(2L).isEmpty());
    }

    @Test
    void tarjetaDeOtroPacienteLanzaDuplicado() throws Exception {
        altaPaciente("12345678Z", "T1");
        altaPaciente("87654321X", "T2");
        assertThrows(PacienteDuplicadoException.class,
                () -> servicio.modificar(ADMIN, 2L, datos("87654321X", "T1")));
    }

    @Test
    void pacienteInexistenteLanzaNoEncontrado() {
        assertThrows(PacienteNoEncontradoException.class,
                () -> servicio.modificar(ADMIN, 99L, datos("12345678Z", "T1")));
    }

    @Test
    void pacienteDeBajaNoSePuedeModificar() throws Exception {
        altaPaciente("12345678Z", "T1");
        repositorio.buscarPorId(1L).orElseThrow().setActivo(false);
        assertThrows(PacienteYaInactivoException.class,
                () -> servicio.modificar(ADMIN, 1L, datos("12345678Z", "T1")));
        assertTrue(repositorio.modificaciones(1L).isEmpty());
    }

    @Test
    void datosInvalidosLanzanDatosInvalidosYNoCambiaNada() throws Exception {
        altaPaciente("12345678Z", "T1");
        Paciente sinNombre = datos("12345678Z", "T1");
        sinNombre.setNombre(" ");
        Paciente dniMalo = datos("123", "T1");
        Paciente emailMalo = datos("12345678Z", "T1");
        emailMalo.setEmail("sin-arroba");
        Paciente fechaFutura = datos("12345678Z", "T1");
        fechaFutura.setFechaNacimiento(LocalDate.now().plusDays(1));
        for (Paciente malo : new Paciente[] {sinNombre, dniMalo, emailMalo, fechaFutura}) {
            assertThrows(DatosInvalidosException.class, () -> servicio.modificar(ADMIN, 1L, malo));
        }
        assertEquals("Ana", repositorio.buscarPorId(1L).orElseThrow().getNombre());
        assertTrue(repositorio.modificaciones(1L).isEmpty());
    }

    @Test
    void telefonoConFormatoInvalidoEsInvalidoYElValidoSeAcepta() throws Exception {
        altaPaciente("12345678Z", "T1");
        for (String tel : new String[] {"abc123", "123", "600-111-222", " "}) {
            Paciente malo = datos("12345678Z", "T1");
            malo.setTelefono(tel);
            assertThrows(DatosInvalidosException.class, () -> servicio.modificar(ADMIN, 1L, malo), tel);
        }
        Paciente bueno = datos("12345678Z", "T1");
        bueno.setTelefono("+34 612345678");
        assertEquals("+34 612345678", servicio.modificar(ADMIN, 1L, bueno).getTelefono());
    }

    @Test
    void soloElPersonalAdministrativoPuedeModificar() throws Exception {
        altaPaciente("12345678Z", "T1");
        for (Rol rol : Rol.values()) {
            if (rol == Rol.PERSONAL_ADMINISTRATIVO) continue;
            assertThrows(SinPermisoException.class,
                    () -> servicio.modificar(new Usuario("x", rol), 1L, datos("12345678Z", "T1")), rol.name());
        }
        assertThrows(SinPermisoException.class, () -> servicio.modificar(null, 1L, datos("12345678Z", "T1")));
    }

    @Test
    void sinPermisoSeRechazaAntesQueInexistenteOInvalido() {
        assertThrows(SinPermisoException.class,
                () -> servicio.modificar(new Usuario("x", Rol.MEDICO), 99L, new Paciente()));
    }

    @Test
    void cadaModificacionGuardaQuienYCuando() throws Exception {
        altaPaciente("12345678Z", "T1");
        servicio.modificar(ADMIN, 1L, datos("12345678Z", "T1"));
        servicio.modificar(ADMIN, 1L, datos("12345678Z", "T1"));
        List<RegistroModificacion> registros = repositorio.modificaciones(1L);
        assertEquals(2, registros.size());
        assertEquals("ana", registros.get(0).usuario());
        assertNotNull(registros.get(0).fecha());
    }

    // ---------- buscar y consultar ----------

    @Test
    void buscarPorNombreApellidosODniSinDistinguirMayusculas() throws Exception {
        altaPaciente("12345678Z", "T1");
        Paciente luis = datos("87654321X", "T2");
        luis.setNombre("Luis");
        luis.setApellidos("García");
        servicio.crear(ADMIN, luis);
        assertEquals(1, servicio.buscar(ADMIN, "ANA").size());
        assertEquals(1, servicio.buscar(ADMIN, "garcía").size());
        assertEquals(1, servicio.buscar(ADMIN, "8765").size());
        assertEquals(0, servicio.buscar(ADMIN, "zzz").size());
    }

    @Test
    void criterioVacioDevuelveListaVacia() throws Exception {
        altaPaciente("12345678Z", "T1");
        assertTrue(servicio.buscar(ADMIN, "").isEmpty());
        assertTrue(servicio.buscar(ADMIN, "   ").isEmpty());
        assertTrue(servicio.buscar(ADMIN, null).isEmpty());
    }

    @Test
    void buscarNoDevuelvePacientesDeBajaPeroConsultarSi() throws Exception {
        altaPaciente("12345678Z", "T1");
        repositorio.buscarPorId(1L).orElseThrow().setActivo(false);
        assertTrue(servicio.buscar(ADMIN, "ana").isEmpty());
        assertEquals("12345678Z", servicio.consultar(ADMIN, 1L).getDni());
    }

    @Test
    void consultarInexistenteLanzaNoEncontrado() {
        assertThrows(PacienteNoEncontradoException.class, () -> servicio.consultar(ADMIN, 99L));
    }
}
