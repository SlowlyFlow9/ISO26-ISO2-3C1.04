package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** CU13 - Cliente: validación de formato de la gestión de solicitudes (D-013). */
class ValidadorGestionSolicitudTest {

    // ---- Modificación ----

    @Test
    @DisplayName("Una modificación con todos los datos obligatorios es válida")
    void modificacionCompletaEsValida() {
        // Ejecutar
        List<String> errores = ValidadorGestionSolicitud.validarModificacion(
                "Dolor", "Control", Prioridad.URGENTE, false, "", null, "");
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("La indicación clínica vacía se rechaza")
    void indicacionVaciaSeRechaza(String indicacion) {
        // Ejecutar
        List<String> errores = ValidadorGestionSolicitud.validarModificacion(
                indicacion, "Control", Prioridad.URGENTE, false, null, null, null);
        // Comprobar
        assertEquals(List.of("La indicación clínica es obligatoria"), errores);
    }

    @Test
    @DisplayName("Sin justificación ni prioridad se devuelven los dos errores")
    void sinJustificacionNiPrioridadDevuelveDosErrores() {
        // Ejecutar
        List<String> errores = ValidadorGestionSolicitud.validarModificacion(
                "Dolor", " ", null, false, null, null, null);
        // Comprobar
        assertEquals(2, errores.size());
    }

    @Test
    @DisplayName("En un seguimiento se valida también la periodicidad")
    void seguimientoValidaLaPeriodicidad() {
        // Ejecutar
        List<String> errores = ValidadorGestionSolicitud.validarModificacion(
                "Dolor", "Control", Prioridad.ORDINARIA, true, "0", null, "abc");
        // Comprobar
        assertEquals(3, errores.size());
    }

    @Test
    @DisplayName("Un seguimiento con periodicidad válida es válido")
    void seguimientoConPeriodicidadValidaEsValido() {
        // Ejecutar
        List<String> errores = ValidadorGestionSolicitud.validarModificacion(
                "Dolor", "Control", Prioridad.ORDINARIA, true, "2", UnidadPeriodicidad.SEMANAS, "5");
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    @Test
    @DisplayName("Si no es un seguimiento, la periodicidad no se valida")
    void sinSeguimientoNoSeValidaLaPeriodicidad() {
        // Ejecutar
        List<String> errores = ValidadorGestionSolicitud.validarModificacion(
                "Dolor", "Control", Prioridad.ORDINARIA, false, "x", null, "-1");
        // Comprobar
        assertTrue(errores.isEmpty());
    }

    // ---- Anulación ----

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("La anulación sin motivo se rechaza")
    void anulacionSinMotivoSeRechaza(String motivo) {
        // Ejecutar y comprobar
        assertEquals(List.of("El motivo de la anulación es obligatorio"),
                ValidadorGestionSolicitud.validarAnulacion(motivo));
    }

    @Test
    @DisplayName("La anulación con motivo es válida")
    void anulacionConMotivoEsValida() {
        // Ejecutar y comprobar
        assertTrue(ValidadorGestionSolicitud.validarAnulacion("Duplicada").isEmpty());
    }

    // ---- Filtro de paciente ----

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1", " 25 "})
    @DisplayName("El filtro de paciente vacío o positivo es válido")
    void filtroPacienteValido(String paciente) {
        // Ejecutar y comprobar
        assertTrue(ValidadorGestionSolicitud.validarFiltroPaciente(paciente).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3", "abc", "1.5"})
    @DisplayName("El filtro de paciente no positivo o no numérico se rechaza")
    void filtroPacienteNoValido(String paciente) {
        // Ejecutar y comprobar
        assertFalse(ValidadorGestionSolicitud.validarFiltroPaciente(paciente).isEmpty());
    }

    // ---- Estados (D-031) ----

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = "PENDIENTE_CITACION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Solo una solicitud pendiente de citación se puede gestionar")
    void soloPendienteCitacionPermiteGestion(EstadoSolicitud estado) {
        // Comprobar
        assertTrue(EstadoSolicitud.PENDIENTE_CITACION.permiteGestion());
        assertFalse(estado.permiteGestion());
    }
}
