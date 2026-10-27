package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PacienteServicioSimuladoTest {

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
    void altaAsignaIdYFechaAlta() throws Exception {
        Paciente r = new PacienteServicioSimulado().alta(paciente("1A", "T1"));
        assertEquals(1L, r.getId());
        assertNotNull(r.getFechaAlta());
    }

    @Test
    void dniRepetidoLanzaDuplicado() throws Exception {
        PacienteServicioSimulado s = new PacienteServicioSimulado();
        s.alta(paciente("1A", "T1"));
        assertThrows(PacienteDuplicadoException.class, () -> s.alta(paciente("1a", "T2")));
    }

    @Test
    void tarjetaRepetidaLanzaDuplicado() throws Exception {
        PacienteServicioSimulado s = new PacienteServicioSimulado();
        s.alta(paciente("1A", "T1"));
        assertThrows(PacienteDuplicadoException.class, () -> s.alta(paciente("2B", "T1")));
    }
}
