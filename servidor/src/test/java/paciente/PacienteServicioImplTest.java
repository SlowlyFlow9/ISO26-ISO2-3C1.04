package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PacienteServicioImplTest {

    private Paciente paciente(String dni, String tarjeta) {
        Paciente p = new Paciente();
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni(dni);
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setNumeroTarjetaSanitaria(tarjeta);
        return p;
    }

    @Test
    void altaAsignaIdFechaAltaYActivo() throws Exception {
        Paciente entrada = paciente("1A", "T1");
        entrada.setId(99L);
        entrada.setActivo(false);
        Paciente r = new PacienteServicioImpl().alta(entrada);
        assertEquals(1L, r.getId());
        assertNotNull(r.getFechaAlta());
        assertTrue(r.isActivo());
    }

    @Test
    void dniRepetidoSinDistinguirMayusculasNiEspacios() throws Exception {
        PacienteServicioImpl s = new PacienteServicioImpl();
        s.alta(paciente("1A", "T1"));
        assertThrows(PacienteDuplicadoException.class, () -> s.alta(paciente(" 1a ", "T2")));
    }

    @Test
    void tarjetaRepetidaLanzaDuplicado() throws Exception {
        PacienteServicioImpl s = new PacienteServicioImpl();
        s.alta(paciente("1A", "T1"));
        assertThrows(PacienteDuplicadoException.class, () -> s.alta(paciente("2B", "T1")));
    }

    @Test
    void faltaObligatorioLanzaArgumentoInvalido() {
        Paciente p = paciente("1A", "T1");
        p.setNombre(" ");
        assertThrows(IllegalArgumentException.class, () -> new PacienteServicioImpl().alta(p));
    }

    @Test
    void altaFallidaNoConsumeId() throws Exception {
        PacienteServicioImpl s = new PacienteServicioImpl();
        s.alta(paciente("1A", "T1"));
        assertThrows(PacienteDuplicadoException.class, () -> s.alta(paciente("1A", "T9")));
        assertEquals(2L, s.alta(paciente("2B", "T2")).getId());
    }
}
