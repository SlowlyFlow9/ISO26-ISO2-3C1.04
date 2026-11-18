package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU12: una solicitud es un seguimiento solo si tiene periodicidad (D-030). */
class SolicitudPruebaTest {

    private SolicitudPrueba sinPeriodicidad() {
        return new SolicitudPrueba(0, 1, 2, 3, "Dolor", "Control", Prioridad.ORDINARIA, null, null);
    }

    private SolicitudPrueba seguimiento(Long origen) {
        return new SolicitudPrueba(0, 1, 2, 3, "Dolor", "Control", Prioridad.ORDINARIA, null, null,
                7, UnidadPeriodicidad.SEMANAS, 3, origen);
    }

    @Test
    @DisplayName("El constructor de CU11 crea una solicitud que no es un seguimiento")
    void constructorDeCu11NoEsSeguimiento() {
        // Ejecutar
        SolicitudPrueba solicitud = sinPeriodicidad();
        // Comprobar
        assertAll(
                () -> assertFalse(solicitud.esSeguimiento()),
                () -> assertNull(solicitud.getIntervalo()),
                () -> assertNull(solicitud.getUnidad()),
                () -> assertNull(solicitud.getNumeroRepeticiones()),
                () -> assertNull(solicitud.getSolicitudOrigenId()));
    }

    @Test
    @DisplayName("El constructor de CU12 crea un seguimiento con su periodicidad")
    void constructorDeCu12EsSeguimiento() {
        // Ejecutar
        SolicitudPrueba solicitud = seguimiento(15L);
        // Comprobar
        assertAll(
                () -> assertTrue(solicitud.esSeguimiento()),
                () -> assertEquals(7, solicitud.getIntervalo()),
                () -> assertEquals(UnidadPeriodicidad.SEMANAS, solicitud.getUnidad()),
                () -> assertEquals(3, solicitud.getNumeroRepeticiones()),
                () -> assertEquals(15L, solicitud.getSolicitudOrigenId()));
    }

    @Test
    @DisplayName("Un seguimiento puede no tener prescripción original")
    void seguimientoSinOrigenSigueSiendoSeguimiento() {
        // Ejecutar
        SolicitudPrueba solicitud = seguimiento(null);
        // Comprobar
        assertAll(
                () -> assertTrue(solicitud.esSeguimiento()),
                () -> assertNull(solicitud.getSolicitudOrigenId()));
    }

    @Test
    @DisplayName("El constructor de CU12 conserva los datos comunes de la solicitud")
    void constructorDeCu12ConservaLosDatosComunes() {
        // Ejecutar
        SolicitudPrueba solicitud = seguimiento(null);
        // Comprobar
        assertAll(
                () -> assertEquals(1, solicitud.getPacienteId()),
                () -> assertEquals(2, solicitud.getMedicoId()),
                () -> assertEquals(3, solicitud.getTipoPruebaId()),
                () -> assertEquals("Dolor", solicitud.getIndicacionClinica()),
                () -> assertEquals("Control", solicitud.getJustificacion()),
                () -> assertEquals(Prioridad.ORDINARIA, solicitud.getPrioridad()));
    }
}
