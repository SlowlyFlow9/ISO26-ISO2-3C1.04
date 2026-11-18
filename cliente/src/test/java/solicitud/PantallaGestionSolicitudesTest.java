package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import paciente.Rol;
import paciente.Usuario;

/** CU13 - Cliente: acceso a la pantalla, botones según el estado y datos de la tabla. */
class PantallaGestionSolicitudesTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 14, 10, 30);

    private static SolicitudPrueba solicitud(long id, EstadoSolicitud estado) {
        return new SolicitudPrueba(id, 10, 7, 3, "Dolor", "Control", Prioridad.URGENTE, FECHA, estado);
    }

    // ---- Acceso ----

    @Test
    @DisplayName("Un médico puede acceder a la gestión de solicitudes")
    void medicoPuedeAcceder() {
        assertTrue(PantallaGestionSolicitudes.disponiblePara(new Usuario("Ana", Rol.MEDICO)));
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "MEDICO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Un usuario sin rol de Médico no puede acceder")
    void otrosRolesNoPuedenAcceder(Rol rol) {
        assertFalse(PantallaGestionSolicitudes.disponiblePara(new Usuario("Luis", rol)));
    }

    @Test
    @DisplayName("Un usuario nulo no puede acceder")
    void usuarioNuloNoPuedeAcceder() {
        assertFalse(PantallaGestionSolicitudes.disponiblePara(null));
    }

    // ---- Botones Modificar y Anular (D-031) ----

    @Test
    @DisplayName("Los botones se activan con una solicitud pendiente de citación")
    void botonesActivosConPendiente() {
        assertTrue(PantallaGestionSolicitudes.botonesActivos(solicitud(1, EstadoSolicitud.PENDIENTE_CITACION)));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = "PENDIENTE_CITACION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los botones están desactivados con una solicitud citada, realizada o anulada")
    void botonesDesactivadosEnOtrosEstados(EstadoSolicitud estado) {
        assertFalse(PantallaGestionSolicitudes.botonesActivos(solicitud(1, estado)));
    }

    @Test
    @DisplayName("Sin solicitud seleccionada los botones están desactivados")
    void botonesDesactivadosSinSeleccion() {
        assertFalse(PantallaGestionSolicitudes.botonesActivos(null));
    }

    // ---- Tabla y detalle ----

    @Test
    @DisplayName("La tabla muestra una fila por solicitud con sus columnas")
    void tablaMuestraLasSolicitudes() {
        // Preparar
        ModeloTablaSolicitudes modelo = new ModeloTablaSolicitudes();
        modelo.setNombreTipo(id -> "TAC");
        SolicitudPrueba seguimiento = new SolicitudPrueba(2, 11, 7, 3, "Control", "Evolución",
                Prioridad.ORDINARIA, FECHA, EstadoSolicitud.CITADA, 1, UnidadPeriodicidad.MESES, 3, null);
        // Ejecutar
        modelo.setSolicitudes(List.of(solicitud(1, EstadoSolicitud.PENDIENTE_CITACION), seguimiento));
        // Comprobar
        assertAll(
                () -> assertEquals(2, modelo.getRowCount()),
                () -> assertEquals(7, modelo.getColumnCount()),
                () -> assertEquals(1L, modelo.getValueAt(0, 0)),
                () -> assertEquals("TAC", modelo.getValueAt(0, 2)),
                () -> assertEquals("14/11/2026 10:30", modelo.getValueAt(0, 4)),
                () -> assertEquals("No", modelo.getValueAt(0, 6)),
                () -> assertEquals(EstadoSolicitud.CITADA, modelo.getValueAt(1, 5)),
                () -> assertEquals("Sí", modelo.getValueAt(1, 6)),
                () -> assertEquals(1, modelo.filaDe(2)),
                () -> assertEquals(-1, modelo.filaDe(99)),
                () -> assertNull(modelo.getSolicitud(5)));
    }

    @Test
    @DisplayName("El detalle de un seguimiento incluye su periodicidad")
    void detalleDeSeguimiento() {
        // Preparar
        SolicitudPrueba seguimiento = new SolicitudPrueba(2, 11, 7, 3, "Control", "Evolución",
                Prioridad.ORDINARIA, FECHA, EstadoSolicitud.PENDIENTE_CITACION, 1, UnidadPeriodicidad.MESES, 3, 1L);
        // Ejecutar
        String detalle = ModeloTablaSolicitudes.detalle(seguimiento, "TAC");
        // Comprobar
        assertAll(
                () -> assertTrue(detalle.contains("Tipo de prueba: TAC")),
                () -> assertTrue(detalle.contains("Seguimiento: cada 1 MESES, 3 repeticiones")),
                () -> assertTrue(detalle.contains("Prescripción original: 1")));
    }

    @Test
    @DisplayName("El detalle de una solicitud anulada incluye el motivo de la anulación")
    void detalleDeAnulada() {
        // Preparar
        SolicitudPrueba anulada = new SolicitudPrueba(3, 11, 7, 3, "Dolor", "Control", Prioridad.URGENTE,
                FECHA, EstadoSolicitud.ANULADA, null, null, null, null, "Duplicada", FECHA.plusDays(1));
        // Ejecutar
        String detalle = ModeloTablaSolicitudes.detalle(anulada, "TAC");
        // Comprobar
        assertAll(
                () -> assertTrue(detalle.contains("Motivo de la anulación: Duplicada")),
                () -> assertTrue(detalle.contains("Fecha de la anulación: 15/11/2026 10:30")));
    }

    @Test
    @DisplayName("Sin solicitud seleccionada el detalle pide seleccionar una")
    void detalleSinSeleccion() {
        assertEquals("Selecciona una solicitud para ver su detalle.", ModeloTablaSolicitudes.detalle(null, ""));
    }
}
