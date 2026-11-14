package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU23: el formulario no se envía con un teléfono de formato inválido. */
class ValidadorPacienteTelefonoTest {
    private static final String ERROR_FORMATO = "El teléfono no tiene un formato válido";

    private Paciente paciente(String telefono) {
        Paciente p = new Paciente();
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni("12345678Z");
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono(telefono);
        return p;
    }

    private void assertTelefonoValido(String telefono) {
        // Preparar
        Paciente p = paciente(telefono);
        // Ejecutar
        List<String> errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    private void assertTelefonoInvalido(String telefono) {
        // Preparar
        Paciente p = paciente(telefono);
        // Ejecutar
        List<String> errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertEquals(List.of(ERROR_FORMATO), errores);
    }

    @Test
    @DisplayName("Un teléfono de 9 dígitos es válido")
    void telefonoDeNueveDigitosEsValido() { assertTelefonoValido("600111222"); }

    @Test
    @DisplayName("Un teléfono con prefijo +34 y espacio es válido")
    void telefonoConPrefijoEsValido() { assertTelefonoValido("+34 612345678"); }

    @Test
    @DisplayName("Un teléfono con espacios alrededor es válido")
    void telefonoConEspaciosAlrededorEsValido() { assertTelefonoValido(" 600111222 "); }

    @Test
    @DisplayName("Un teléfono con letras y dígitos es inválido")
    void telefonoConLetrasEsInvalido() { assertTelefonoInvalido("abc123"); }

    @Test
    @DisplayName("Un teléfono demasiado corto es inválido")
    void telefonoCortoEsInvalido() { assertTelefonoInvalido("123"); }

    @Test
    @DisplayName("Un teléfono con guiones es inválido")
    void telefonoConGuionesEsInvalido() { assertTelefonoInvalido("600-111-222"); }

    @Test
    @DisplayName("Un teléfono en blanco sigue siendo obligatorio")
    void telefonoVacioSigueSiendoObligatorio() {
        // Preparar
        Paciente p = paciente(" ");
        // Ejecutar
        List<String> errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertEquals(List.of("El teléfono es obligatorio"), errores);
    }
}
