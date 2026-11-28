package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidadorReglaPrioridadTest {

    private final ValidadorReglaPrioridad validador =
            new ValidadorReglaPrioridad();

    @Test
    void validaDatosYRecortaTexto() {
        ValidadorReglaPrioridad.Resultado resultado =
                validador.validar(" Urgente ", " prioridad alta ", " 1 ");

        assertTrue(resultado.esValido());
        assertEquals("Urgente", resultado.getDatos().getNombre());
        assertEquals("prioridad alta",
                resultado.getDatos().getDescripcion());
        assertEquals(1, resultado.getDatos().getOrden());
    }

    @Test
    void permiteDescripcionOpcional() {
        ValidadorReglaPrioridad.Resultado resultado =
                validador.validar("Urgente", null, "2");

        assertTrue(resultado.esValido());
        assertEquals("", resultado.getDatos().getDescripcion());
    }

    @Test
    void nombreVacioEsInvalido() {
        ValidadorReglaPrioridad.Resultado resultado =
                validador.validar("  ", "", "1");

        assertFalse(resultado.esValido());
        assertTrue(resultado.getErrores().get(0)
                .contains("nombre"));
        assertNull(resultado.getDatos());
    }

    @Test
    void ordenDebeSerUnEnteroPositivo() {
        assertTrue(validador.validar("Urgente", "", "")
                .getErrores().get(0).contains("obligatorio"));
        assertTrue(validador.validar("Urgente", "", "abc")
                .getErrores().get(0).contains("entero"));
        assertTrue(validador.validar("Urgente", "", "0")
                .getErrores().get(0).contains("mayor que cero"));
        assertTrue(validador.validar("Urgente", "", "-1")
                .getErrores().get(0).contains("mayor que cero"));
        assertFalse(validador.validar(null, null, null).esValido());
    }
}
