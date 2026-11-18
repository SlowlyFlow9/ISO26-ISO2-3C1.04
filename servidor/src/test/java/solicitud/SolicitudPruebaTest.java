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
    @DisplayName("conId conserva la periodicidad del seguimiento")
    void conIdConservaLaPeriodicidad() {
        // Preparar
        SolicitudPrueba solicitud = seguimiento(15L);
        // Ejecutar
        SolicitudPrueba conId = solicitud.conId(9);
        // Comprobar
        assertAll(
                () -> assertEquals(9, conId.getId()),
                () -> assertTrue(conId.esSeguimiento()),
                () -> assertEquals(7, conId.getIntervalo()),
                () -> assertEquals(UnidadPeriodicidad.SEMANAS, conId.getUnidad()),
                () -> assertEquals(3, conId.getNumeroRepeticiones()),
                () -> assertEquals(15L, conId.getSolicitudOrigenId()));
    }

    @Test
    @DisplayName("conId en una solicitud sin periodicidad sigue sin ser un seguimiento")
    void conIdSinPeriodicidadNoEsSeguimiento() {
        // Preparar
        SolicitudPrueba solicitud = sinPeriodicidad();
        // Ejecutar
        SolicitudPrueba conId = solicitud.conId(9);
        // Comprobar
        assertFalse(conId.esSeguimiento());
    }
}
