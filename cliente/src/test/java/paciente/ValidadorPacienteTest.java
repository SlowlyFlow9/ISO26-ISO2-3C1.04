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
        p.setNumeroTarjetaSanitaria("CLM0001");
        return p;
    }

    @Test
    void pacienteCompletoNoTieneErrores() {
        assertTrue(ValidadorPaciente.validar(valido()).isEmpty());
    }

    @Test
    void opcionalesVaciosSonValidos() {
        Paciente p = valido();
        p.setTelefono(null); p.setEmail(""); p.setDireccion(" ");
        assertTrue(ValidadorPaciente.validar(p).isEmpty());
    }

    @Test
    void cadaObligatorioVacioGeneraUnError() {
        Paciente p = new Paciente();
        p.setNombre("  ");
        assertEquals(5, ValidadorPaciente.validar(p).size());
    }
}
