package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** CU28 - Cliente: validación de formato del cambio de prioridad (D-013). */
class ValidadorCambioPrioridadTest {

    private static final ReglaPrioridadAplicable REGLA =
            new ReglaPrioridadAplicable(1, "Paciente oncológico", "", 1);

    @Test
    @DisplayName("Un cambio con prioridad distinta, regla y motivo es válido")
    void cambioValido() {
        // Ejecutar
        List<String> errores = ValidadorCambioPrioridad.validar(
                Prioridad.URGENTE, Prioridad.ORDINARIA, REGLA, "Empeoramiento");
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    @Test
    @DisplayName("Sin nueva prioridad se pide seleccionarla")
    void sinNuevaPrioridad() {
        assertEquals(List.of("Selecciona la nueva prioridad"),
                ValidadorCambioPrioridad.validar(null, Prioridad.ORDINARIA, REGLA, "Motivo"));
    }

    @Test
    @DisplayName("La misma prioridad que la actual se rechaza")
    void mismaPrioridad() {
        assertEquals(List.of("La nueva prioridad debe ser distinta de la actual"),
                ValidadorCambioPrioridad.validar(Prioridad.URGENTE, Prioridad.URGENTE, REGLA, "Motivo"));
    }

    @Test
    @DisplayName("Sin regla de prioridad se pide seleccionarla")
    void sinRegla() {
        assertEquals(List.of("Selecciona la regla de prioridad aplicada"),
                ValidadorCambioPrioridad.validar(Prioridad.URGENTE, Prioridad.ORDINARIA, null, "Motivo"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("El motivo vacío se rechaza")
    void motivoVacio(String motivo) {
        assertEquals(List.of("El motivo del cambio es obligatorio"),
                ValidadorCambioPrioridad.validar(Prioridad.URGENTE, Prioridad.ORDINARIA, REGLA, motivo));
    }

    @Test
    @DisplayName("Si falta todo se devuelven los tres errores")
    void faltaTodo() {
        assertEquals(3, ValidadorCambioPrioridad.validar(null, Prioridad.ORDINARIA, null, "").size());
    }

    @Test
    @DisplayName("La regla se muestra por su nombre en el desplegable")
    void reglaSeMuestraPorNombre() {
        assertEquals("Paciente oncológico", REGLA.toString());
    }
}
