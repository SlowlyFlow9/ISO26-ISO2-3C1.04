package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Un paciente completo no tiene errores")
    void pacienteCompletoNoTieneErrores() {
        // Preparar
        Paciente p = valido();
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    @Test
    @DisplayName("Los campos opcionales vacíos son válidos")
    void opcionalesVaciosSonValidos() {
        // Preparar
        Paciente p = valido();
        p.setEmail("");
        p.setDireccion(" ");
        p.setNumeroTarjetaSanitaria(null);
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    @Test
    @DisplayName("Cada campo obligatorio vacío genera un error")
    void cadaObligatorioVacioGeneraUnError() {
        // Preparar
        Paciente p = new Paciente();
        p.setNombre("  ");
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertEquals(5, errores.size());
    }

    // ---- DNI / NIE ----

    @Test
    @DisplayName("Un DNI válido pasa")
    void dniValidoPasa() { assertDniValido("12345678Z"); }

    @Test
    @DisplayName("Un NIE válido pasa")
    void nieValidoPasa() { assertDniValido("X1234567L"); }

    @Test
    @DisplayName("Un NIE en minúsculas y con espacios pasa")
    void nieEnMinusculasYConEspaciosPasa() { assertDniValido(" y1234567x "); }

    private void assertDniValido(String dni) {
        // Preparar
        Paciente p = valido();
        p.setDni(dni);
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    @Test
    @DisplayName("Un DNI con 7 dígitos y letra genera un error")
    void dniConSieteDigitosGeneraError() { assertDniInvalido("1234567Z"); }

    @Test
    @DisplayName("Un DNI con 9 dígitos y sin letra genera un error")
    void dniSoloNumericoGeneraError() { assertDniInvalido("123456789"); }

    @Test
    @DisplayName("Un DNI con solo letras genera un error")
    void dniSoloLetrasGeneraError() { assertDniInvalido("ABCDEFGHI"); }

    @Test
    @DisplayName("Un NIE con 8 dígitos genera un error")
    void nieConOchoDigitosGeneraError() { assertDniInvalido("X12345678"); }

    private void assertDniInvalido(String dni) {
        // Preparar
        Paciente p = valido();
        p.setDni(dni);
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertEquals(1, errores.size());
    }

    // ---- Fecha de nacimiento ----

    @Test
    @DisplayName("Una fecha de nacimiento futura (mañana) genera un error")
    void fechaNacimientoFuturaGeneraError() {
        // Preparar
        Paciente p = valido();
        p.setFechaNacimiento(LocalDate.now().plusDays(1));
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertEquals(1, errores.size());
    }

    @Test
    @DisplayName("Una fecha de nacimiento de hoy es válida (valor límite)")
    void fechaNacimientoDeHoyEsValida() {
        // Preparar
        Paciente p = valido();
        p.setFechaNacimiento(LocalDate.now());
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    // ---- Email ----

    @Test
    @DisplayName("Un email sin arroba genera un error")
    void emailSinArrobaGeneraError() {
        // Preparar
        Paciente p = valido();
        p.setEmail("sin-arroba");
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertEquals(1, errores.size());
    }

    @Test
    @DisplayName("Un email con formato correcto es válido")
    void emailCorrectoEsValido() {
        // Preparar
        Paciente p = valido();
        p.setEmail("ana@correo.es");
        // Ejecutar
        var errores = ValidadorPaciente.validar(p);
        // Comprobar
        assertTrue(errores.isEmpty());
    }
}
