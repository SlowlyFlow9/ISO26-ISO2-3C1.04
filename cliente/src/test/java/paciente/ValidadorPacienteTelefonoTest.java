package paciente;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU23: el formulario no se envía con un teléfono de formato inválido. */
class ValidadorPacienteTelefonoTest {

    private Paciente valido() {
        Paciente p = new Paciente();
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni("12345678Z");
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono("600111222");
        return p;
    }

    // ---- Teléfono válido ----

    @Test
    @DisplayName("Un teléfono de 9 dígitos pasa")
    void telefonoDeNueveDigitosPasa() { assertTelefonoValido("600111222"); }

    @Test
    @DisplayName("Un teléfono con prefijo +34 pasa")
    void telefonoConPrefijoPasa() { assertTelefonoValido("+34 612345678"); }

    @Test
    @DisplayName("Un teléfono con espacios alrededor pasa")
    void telefonoConEspaciosAlrededorPasa() { assertTelefonoValido(" 600111222 "); }

    private void assertTelefonoValido(String telefono) {
        // Preparar
        Paciente p = valido();
        p.setTelefono(telefono);
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    // ---- Teléfono con formato inválido ----

    @Test
    @DisplayName("Un teléfono con letras genera un error de formato")
    void telefonoConLetrasGeneraError() { assertTelefonoInvalido("abc123"); }

    @Test
    @DisplayName("Un teléfono demasiado corto genera un error de formato")
    void telefonoDemasiadoCortoGeneraError() { assertTelefonoInvalido("123"); }

    @Test
    @DisplayName("Un teléfono con guiones genera un error de formato")
    void telefonoConGuionesGeneraError() { assertTelefonoInvalido("600-111-222"); }

    private void assertTelefonoInvalido(String telefono) {
        // Preparar
        Paciente p = valido();
        p.setTelefono(telefono);
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertEquals(List.of("El teléfono no tiene un formato válido"), errores);
    }

    // ---- Teléfono obligatorio ----

    @Test
    @DisplayName("Un teléfono en blanco genera el error de obligatorio")
    void telefonoEnBlancoGeneraErrorDeObligatorio() {
        // Preparar
        Paciente p = valido();
        p.setTelefono(" ");
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertEquals(List.of("El teléfono es obligatorio"), errores);
    }
}
