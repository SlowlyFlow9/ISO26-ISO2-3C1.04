package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ValidadorPoliticaCitaTest {

    @Test
    void parsearValoresEnteros() throws Exception {
        PoliticaCita politica = ValidadorPoliticaCita.parsear(
                "0", "90", "24", "3");

        assertEquals(0, politica.getAntelacionMinimaHoras());
        assertEquals(90, politica.getAntelacionMaximaDias());
        assertEquals(24, politica.getCancelacionMinimaHoras());
        assertEquals(3, politica.getMaxCitasActivasPorPaciente());
    }

    @Test
    void rechazarCamposVacios() {
        assertThrows(FormatoPoliticaCitaException.class,
                () -> ValidadorPoliticaCita.parsear(
                        "", "90", "24", "3"));
    }

    @Test
    void rechazarTextoNoNumerico() {
        assertThrows(FormatoPoliticaCitaException.class,
                () -> ValidadorPoliticaCita.parsear(
                        "cero", "90", "24", "3"));
    }

    @Test
    void elClienteNoDuplicaValidacionDeNegocio()
            throws Exception {
        PoliticaCita politica = ValidadorPoliticaCita.parsear(
                "-1", "1", "48", "0");

        assertEquals(-1, politica.getAntelacionMinimaHoras());
        assertEquals(0, politica.getMaxCitasActivasPorPaciente());
    }
}
