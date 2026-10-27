package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PacientesServiceImplTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesServiceImpl servicio() { return new PacientesServiceImpl(new PacientesRepositorioMemoria()); }

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

    @Test
    void altaCorrectaAsignaIdFechaAltaYActivo() throws Exception {
        Paciente entrada = valido();
        entrada.setId(99L);
        entrada.setActivo(false);
        Paciente r = servicio().crear(ADMIN, entrada);
        assertEquals(1L, r.getId());
        assertNotNull(r.getFechaAlta());
        assertTrue(r.isActivo());
    }

    @Test
    void elPacienteCreadoQuedaDisponibleEnElRepositorio() throws Exception {
        PacientesRepositorioMemoria repositorio = new PacientesRepositorioMemoria();
        Paciente r = new PacientesServiceImpl(repositorio).crear(ADMIN, valido());
        assertEquals("12345678Z", repositorio.buscarPorId(r.getId()).orElseThrow().getDni());
    }

    @Test
    void dniRepetidoSinDistinguirMayusculasNiEspacios() throws Exception {
        PacientesServiceImpl s = servicio();
        s.crear(ADMIN, paciente("X1234567L", "T1"));
        assertThrows(PacienteDuplicadoException.class, () -> s.crear(ADMIN, paciente(" x1234567l ", "T2")));
    }

    @Test
    void tarjetaRepetidaLanzaDuplicado() throws Exception {
        PacientesServiceImpl s = servicio();
        s.crear(ADMIN, valido());
        assertThrows(PacienteDuplicadoException.class, () -> s.crear(ADMIN, paciente("87654321X", "T1")));
    }

    @Test
    void tarjetaOpcionalYVariasSinTarjetaNoSeConsideranDuplicadas() throws Exception {
        PacientesServiceImpl s = servicio();
        s.crear(ADMIN, paciente("12345678Z", null));
        s.crear(ADMIN, paciente("87654321X", " "));
    }

    @Test
    void camposObligatoriosVaciosDanUnErrorCadaUno() {
        DatosInvalidosException e = assertThrows(DatosInvalidosException.class, () -> servicio().crear(ADMIN, new Paciente()));
        assertEquals(5, e.getErrores().size());
    }

    @Test
    void telefonoVacioEsInvalido() {
        Paciente p = valido();
        p.setTelefono(" ");
        assertThrows(DatosInvalidosException.class, () -> servicio().crear(ADMIN, p));
    }

    @Test
    void dniConFormatoInvalidoEsInvalido() {
        for (String dni : new String[] {"1234567Z", "123456789", "ABCDEFGHI", "X12345678"}) {
            assertThrows(DatosInvalidosException.class, () -> servicio().crear(ADMIN, paciente(dni, "T1")), dni);
        }
    }

    @Test
    void emailInvalidoEsInvalidoYVacioEsValido() throws Exception {
        Paciente malo = valido();
        malo.setEmail("sin-arroba");
        assertThrows(DatosInvalidosException.class, () -> servicio().crear(ADMIN, malo));
        Paciente bueno = valido();
        bueno.setEmail("ana@correo.es");
        servicio().crear(ADMIN, bueno);
        Paciente vacio = valido();
        vacio.setEmail("");
        servicio().crear(ADMIN, vacio);
    }

    @Test
    void fechaNacimientoFuturaEsInvalida() {
        Paciente p = valido();
        p.setFechaNacimiento(LocalDate.now().plusDays(1));
        assertThrows(DatosInvalidosException.class, () -> servicio().crear(ADMIN, p));
    }

    @Test
    void fechaNacimientoDeHoyEsValida() throws Exception {
        Paciente p = valido();
        p.setFechaNacimiento(LocalDate.now());
        servicio().crear(ADMIN, p);
    }

    @Test
    void soloElPersonalAdministrativoPuedeDarDeAlta() {
        for (Rol rol : Rol.values()) {
            if (rol == Rol.PERSONAL_ADMINISTRATIVO) continue;
            assertThrows(SinPermisoException.class, () -> servicio().crear(new Usuario("x", rol), valido()), rol.name());
        }
        assertThrows(SinPermisoException.class, () -> servicio().crear(null, valido()));
    }

    @Test
    void sinPermisoSeRechazaAntesQueLosDatosInvalidos() {
        assertThrows(SinPermisoException.class, () -> servicio().crear(new Usuario("x", Rol.MEDICO), new Paciente()));
    }

    @Test
    void altaFallidaNoConsumeId() throws Exception {
        PacientesServiceImpl s = servicio();
        s.crear(ADMIN, valido());
        assertThrows(PacienteDuplicadoException.class, () -> s.crear(ADMIN, paciente("12345678Z", "T9")));
        assertEquals(2L, s.crear(ADMIN, paciente("87654321X", "T2")).getId());
    }
}
