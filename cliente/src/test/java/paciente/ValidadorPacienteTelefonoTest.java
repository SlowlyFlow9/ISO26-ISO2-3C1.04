package paciente;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** CU23: el formulario no se envía con un teléfono de formato inválido. */
class ValidadorPacienteTelefonoTest {

    private Paciente paciente(String telefono) {
        Paciente p = new Paciente();
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni("12345678Z");
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono(telefono);
        return p;
    }

    @Test
    void telefonosConFormatoValidoSeAceptan() {
        for (String tel : new String[] {"600111222", "+34 612345678", " 600111222 "}) {
            assertTrue(ValidadorPaciente.validar(paciente(tel)).isEmpty(), tel);
        }
    }

    @Test
    void telefonosConFormatoInvalidoDanUnError() {
        for (String tel : new String[] {"abc123", "123", "600-111-222"}) {
            List<String> errores = ValidadorPaciente.validar(paciente(tel));
            assertEquals(1, errores.size(), tel);
            assertEquals("El teléfono no tiene un formato válido", errores.get(0));
        }
    }

    @Test
    void telefonoVacioSigueSiendoObligatorio() {
        List<String> errores = ValidadorPaciente.validar(paciente(" "));
        assertEquals(List.of("El teléfono es obligatorio"), errores);
    }
}
