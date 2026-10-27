package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ValidadorPacienteTest {

    private Paciente valido() {
        Paciente p = new Paciente();
        p.setNombre("Ana");
        p.setApellidos("Pérez Gil");
        p.setDni("12345678Z");
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono("600111222");
        return p;
    }

    @Test
    void pacienteCompletoNoTieneErrores() {
        assertTrue(ValidadorPaciente.validar(valido()).isEmpty());
    }

    @Test
    void opcionalesVaciosSonValidos() {
        Paciente p = valido();
        p.setEmail("");
        p.setDireccion(" ");
        p.setNumeroTarjetaSanitaria(null);
        assertTrue(ValidadorPaciente.validar(p).isEmpty());
    }

    @Test
    void cadaObligatorioVacioGeneraUnError() {
        Paciente p = new Paciente();
        p.setNombre("  ");
        assertEquals(5, ValidadorPaciente.validar(p).size());
    }

    @Test
    void dniYNieValidosPasan() {
        for (String dni : new String[] {"12345678Z", "X1234567L", " y1234567x "}) {
            Paciente p = valido();
            p.setDni(dni);
            assertTrue(ValidadorPaciente.validar(p).isEmpty(), dni);
        }
    }

    @Test
    void dniConFormatoInvalidoGeneraError() {
        for (String dni : new String[] {"1234567Z", "123456789", "ABCDEFGHI", "X12345678"}) {
            Paciente p = valido();
            p.setDni(dni);
            assertEquals(1, ValidadorPaciente.validar(p).size(), dni);
        }
    }

    @Test
    void emailInvalidoGeneraError() {
        Paciente p = valido();
        p.setEmail("sin-arroba");
        assertEquals(1, ValidadorPaciente.validar(p).size());
        p.setEmail("ana@correo.es");
        assertTrue(ValidadorPaciente.validar(p).isEmpty());
    }
}
