package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU12: validación del formato de la periodicidad de una prueba de seguimiento (D-013). */
class ValidadorSeguimientoTest {
    private static final String ERROR_INTERVALO = "El intervalo debe ser un número entero mayor que 0";
    private static final String ERROR_UNIDAD = "Selecciona la unidad de la periodicidad";
    private static final String ERROR_REPETICIONES = "El número de repeticiones debe ser un número entero mayor que 0";
    private static final String ERROR_ORIGEN = "La prescripción original debe ser un identificador positivo";

    private void assertValido(String intervalo, UnidadPeriodicidad unidad, String repeticiones, String origen) {
        // Ejecutar
        List<String> errores = ValidadorSeguimiento.validar(intervalo, unidad, repeticiones, origen);
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    private void assertUnError(String intervalo, UnidadPeriodicidad unidad, String repeticiones, String origen,
            String esperado) {
        // Ejecutar
        List<String> errores = ValidadorSeguimiento.validar(intervalo, unidad, repeticiones, origen);
        // Comprobar
        assertEquals(List.of(esperado), errores);
    }

    // ---- Datos válidos ----

    @Test
    @DisplayName("Una periodicidad completa sin prescripción original es válida")
    void periodicidadCompletaEsValida() { assertValido("7", UnidadPeriodicidad.DIAS, "3", ""); }

    @Test
    @DisplayName("Una periodicidad completa con prescripción original es válida")
    void periodicidadConOrigenEsValida() { assertValido("2", UnidadPeriodicidad.MESES, "4", "15"); }

    @Test
    @DisplayName("Un intervalo de 1 es válido (valor límite)")
    void intervaloUnoEsValido() { assertValido("1", UnidadPeriodicidad.SEMANAS, "1", null); }

    @Test
    @DisplayName("Los espacios alrededor de los números se ignoran")
    void espaciosAlrededorSeIgnoran() { assertValido(" 7 ", UnidadPeriodicidad.DIAS, " 3 ", " 15 "); }

    // ---- Intervalo ----

    @Test
    @DisplayName("Un intervalo de 0 es inválido (valor límite)")
    void intervaloCeroEsInvalido() { assertUnError("0", UnidadPeriodicidad.DIAS, "3", "", ERROR_INTERVALO); }

    @Test
    @DisplayName("Un intervalo negativo es inválido")
    void intervaloNegativoEsInvalido() { assertUnError("-1", UnidadPeriodicidad.DIAS, "3", "", ERROR_INTERVALO); }

    @Test
    @DisplayName("Un intervalo no numérico es inválido")
    void intervaloConLetrasEsInvalido() { assertUnError("abc", UnidadPeriodicidad.DIAS, "3", "", ERROR_INTERVALO); }

    @Test
    @DisplayName("Un intervalo decimal es inválido")
    void intervaloDecimalEsInvalido() { assertUnError("1.5", UnidadPeriodicidad.DIAS, "3", "", ERROR_INTERVALO); }

    @Test
    @DisplayName("Un intervalo vacío es inválido")
    void intervaloVacioEsInvalido() { assertUnError(" ", UnidadPeriodicidad.DIAS, "3", "", ERROR_INTERVALO); }

    @Test
    @DisplayName("Un intervalo nulo es inválido")
    void intervaloNuloEsInvalido() { assertUnError(null, UnidadPeriodicidad.DIAS, "3", "", ERROR_INTERVALO); }

    // ---- Unidad ----

    @Test
    @DisplayName("Sin unidad seleccionada hay un error")
    void unidadNulaEsInvalida() { assertUnError("7", null, "3", "", ERROR_UNIDAD); }

    // ---- Repeticiones ----

    @Test
    @DisplayName("Un número de repeticiones de 0 es inválido (valor límite)")
    void repeticionesCeroEsInvalido() { assertUnError("7", UnidadPeriodicidad.DIAS, "0", "", ERROR_REPETICIONES); }

    @Test
    @DisplayName("Un número de repeticiones no numérico es inválido")
    void repeticionesConLetrasEsInvalido() { assertUnError("7", UnidadPeriodicidad.DIAS, "x", "", ERROR_REPETICIONES); }

    @Test
    @DisplayName("Un número de repeticiones vacío es inválido")
    void repeticionesVaciasEsInvalido() { assertUnError("7", UnidadPeriodicidad.DIAS, "", "", ERROR_REPETICIONES); }

    // ---- Prescripción original ----

    @Test
    @DisplayName("Una prescripción original de 0 es inválida (valor límite)")
    void origenCeroEsInvalido() { assertUnError("7", UnidadPeriodicidad.DIAS, "3", "0", ERROR_ORIGEN); }

    @Test
    @DisplayName("Una prescripción original no numérica es inválida")
    void origenConLetrasEsInvalido() { assertUnError("7", UnidadPeriodicidad.DIAS, "3", "abc", ERROR_ORIGEN); }

    @Test
    @DisplayName("Una prescripción original en blanco es válida porque es opcional")
    void origenEnBlancoEsValido() { assertValido("7", UnidadPeriodicidad.DIAS, "3", "   "); }

    // ---- Varios errores ----

    @Test
    @DisplayName("Con todos los datos inválidos hay un error por cada campo")
    void todosInvalidosDanUnErrorCadaUno() {
        // Ejecutar
        List<String> errores = ValidadorSeguimiento.validar("0", null, "0", "abc");
        // Comprobar
        assertEquals(List.of(ERROR_INTERVALO, ERROR_UNIDAD, ERROR_REPETICIONES, ERROR_ORIGEN), errores);
    }
}
