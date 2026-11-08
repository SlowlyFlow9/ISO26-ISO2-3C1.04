package tipoprueba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidadorTipoPruebaTest {
    private final ValidadorTipoPrueba v = new ValidadorTipoPrueba();

    @Test
    void datosCorrectosSonValidos() {
        ValidadorTipoPrueba.Resultado r = v.validar("TAC", "Craneal", "30", false, "");
        assertTrue(r.esValido());
        assertEquals(30, r.getDatos().getDuracionEstimada());
    }

    @Test
    void nombreVacioEsInvalido() {
        assertFalse(v.validar("  ", "", "30", false, "").esValido());
    }

    @Test
    void duracionCeroNegativaOVaciaEsInvalida() {
        assertFalse(v.validar("TAC", "", "0", false, "").esValido());
        assertFalse(v.validar("TAC", "", "-5", false, "").esValido());
        assertFalse(v.validar("TAC", "", "", false, "").esValido());
    }

    @Test
    void duracionNoNumericaEsInvalida() {
        assertEquals(Mensajes.DURACION_NO_NUMERICA, v.validar("TAC", "", "abc", false, "").getErrores().get(0));
    }

    @Test
    void preparacionSinInstruccionesEsInvalida() {
        assertFalse(v.validar("RM", "", "45", true, "").esValido());
    }
}
