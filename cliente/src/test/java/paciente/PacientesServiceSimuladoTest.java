package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PacientesServiceSimuladoTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

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

    @Test
    void altaAsignaIdFechaAltaYActivo() throws Exception {
        Paciente entrada = paciente("12345678Z", "T1");
        entrada.setActivo(false);
        Paciente r = new PacientesServiceSimulado().crear(ADMIN, entrada);
        assertEquals(1L, r.getId());
        assertNotNull(r.getFechaAlta());
        assertTrue(r.isActivo());
    }

    @Test
    void dniRepetidoLanzaDuplicado() throws Exception {
        PacientesServiceSimulado s = new PacientesServiceSimulado();
        s.crear(ADMIN, paciente("12345678Z", "T1"));
        assertThrows(PacienteDuplicadoException.class, () -> s.crear(ADMIN, paciente("12345678z", "T2")));
    }

    @Test
    void tarjetaRepetidaLanzaDuplicado() throws Exception {
        PacientesServiceSimulado s = new PacientesServiceSimulado();
        s.crear(ADMIN, paciente("12345678Z", "T1"));
        assertThrows(PacienteDuplicadoException.class, () -> s.crear(ADMIN, paciente("87654321X", "T1")));
    }

    @Test
    void variosSinTarjetaNoSonDuplicados() throws Exception {
        PacientesServiceSimulado s = new PacientesServiceSimulado();
        s.crear(ADMIN, paciente("12345678Z", null));
        s.crear(ADMIN, paciente("87654321X", ""));
    }

    @Test
    void datosInvalidosLanzanLaExcepcionConLosErrores() {
        Paciente p = paciente("123", "T1");
        DatosInvalidosException e = assertThrows(DatosInvalidosException.class, () -> new PacientesServiceSimulado().crear(ADMIN, p));
        assertEquals(1, e.getErrores().size());
    }

    @Test
    void fechaNacimientoFuturaEsInvalida() {
        Paciente p = paciente("12345678Z", "T1");
        p.setFechaNacimiento(LocalDate.now().plusDays(1));
        assertThrows(DatosInvalidosException.class, () -> new PacientesServiceSimulado().crear(ADMIN, p));
    }

    @Test
    void soloElPersonalAdministrativoPuedeDarDeAlta() {
        for (Rol rol : Rol.values()) {
            if (rol == Rol.PERSONAL_ADMINISTRATIVO) continue;
            assertThrows(SinPermisoException.class,
                    () -> new PacientesServiceSimulado().crear(new Usuario("x", rol), paciente("12345678Z", "T1")), rol.name());
        }
    }
}
