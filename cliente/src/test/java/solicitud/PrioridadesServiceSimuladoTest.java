package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import paciente.Rol;
import paciente.Usuario;

/** CU28 - Cliente: reglas de la gestión de prioridades en el servicio simulado (D-038). */
class PrioridadesServiceSimuladoTest {

    private static final Usuario ADMINISTRATIVO = new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 20, 9, 0);

    private PrioridadesServiceSimulado servicio;

    @BeforeEach
    void setUp() {
        servicio = new PrioridadesServiceSimulado()
                .conRegla(new ReglaPrioridadAplicable(1, "Oncológico", "", 2), true)
                .conRegla(new ReglaPrioridadAplicable(2, "Espera prolongada", "", 1), true)
                .conRegla(new ReglaPrioridadAplicable(3, "Retirada", "", 3), false)
                .conSolicitud(new SolicitudPrioridad(1, 10, 1, Prioridad.ORDINARIA, FECHA), true)
                .conSolicitud(new SolicitudPrioridad(2, 20, 1, Prioridad.URGENTE, FECHA.plusDays(1)), true)
                .conSolicitud(new SolicitudPrioridad(3, 30, 1, Prioridad.URGENTE, FECHA), true)
                .conSolicitud(new SolicitudPrioridad(4, 40, 1, Prioridad.PREFERENTE, FECHA), false);
    }

    private static List<Long> ids(List<SolicitudPrioridad> lista) {
        return lista.stream().map(SolicitudPrioridad::solicitudId).toList();
    }

    private static DatosCambioPrioridad cambio(Prioridad prioridad, long regla, String motivo) {
        return new DatosCambioPrioridad(prioridad, regla, motivo);
    }

    // ---- Consulta ----

    @Test
    @DisplayName("Solo se listan las solicitudes pendientes, ordenadas por prioridad, fecha e id")
    void listaPendientesOrdenadas() {
        assertEquals(List.of(3L, 2L, 1L), ids(servicio.consultarPendientes(ADMINISTRATIVO, null)));
    }

    @Test
    @DisplayName("El filtro por prioridad devuelve solo las solicitudes con esa prioridad")
    void filtroPorPrioridad() {
        assertAll(
                () -> assertEquals(List.of(3L, 2L), ids(servicio.consultarPendientes(ADMINISTRATIVO, Prioridad.URGENTE))),
                () -> assertTrue(servicio.consultarPendientes(ADMINISTRATIVO, Prioridad.PREFERENTE).isEmpty()));
    }

    @Test
    @DisplayName("Solo se devuelven las reglas activas, ordenadas por su orden")
    void reglasActivasOrdenadas() {
        assertEquals(List.of(2L, 1L), servicio.consultarReglasActivas(ADMINISTRATIVO).stream()
                .map(ReglaPrioridadAplicable::id).toList());
    }

    // ---- Cambio de prioridad ----

    @Test
    @DisplayName("Cambiar la prioridad la actualiza, conserva el resto y reordena la lista")
    void cambiarPrioridad() {
        // Ejecutar
        SolicitudPrioridad actualizada = servicio.cambiarPrioridad(ADMINISTRATIVO, 1,
                cambio(Prioridad.URGENTE, 1, "  Empeoramiento  "));
        // Comprobar
        assertAll(
                () -> assertEquals(Prioridad.URGENTE, actualizada.prioridad()),
                () -> assertEquals(10, actualizada.pacienteId()),
                () -> assertEquals(FECHA, actualizada.fechaSolicitud()),
                () -> assertEquals(List.of(1L, 3L, 2L), ids(servicio.consultarPendientes(ADMINISTRATIVO, null))));
    }

    @Test
    @DisplayName("Cada cambio queda en el historial, del más reciente al más antiguo")
    void historialDeCambios() {
        // Preparar
        servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.PREFERENTE, 2, "Espera"));
        servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.URGENTE, 1, "Empeoramiento"));
        // Ejecutar
        List<CambioPrioridad> historial = servicio.consultarHistorial(ADMINISTRATIVO, 1);
        // Comprobar
        CambioPrioridad ultimo = historial.get(0);
        assertAll(
                () -> assertEquals(2, historial.size()),
                () -> assertEquals(Prioridad.PREFERENTE, ultimo.prioridadAnterior()),
                () -> assertEquals(Prioridad.URGENTE, ultimo.prioridadNueva()),
                () -> assertEquals(1, ultimo.reglaPrioridadId()),
                () -> assertEquals("Empeoramiento", ultimo.motivo()),
                () -> assertEquals("admin", ultimo.realizadoPor()),
                () -> assertNotNull(ultimo.fechaCambio()),
                () -> assertEquals(Prioridad.ORDINARIA, historial.get(1).prioridadAnterior()));
    }

    @Test
    @DisplayName("Una solicitud sin cambios tiene el historial vacío")
    void historialVacio() {
        assertTrue(servicio.consultarHistorial(ADMINISTRATIVO, 2).isEmpty());
    }

    @Test
    @DisplayName("No se puede cambiar la prioridad de una solicitud que ya no está pendiente")
    void solicitudNoPendiente() {
        assertThrows(IllegalStateException.class,
                () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 4, cambio(Prioridad.URGENTE, 1, "Motivo")));
    }

    @Test
    @DisplayName("Una regla retirada, una prioridad igual, sin prioridad o sin motivo se rechazan y no cambian nada")
    void cambiosNoValidos() {
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.URGENTE, 3, "M"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.ORDINARIA, 1, "M"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(null, 1, "M"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.URGENTE, 1, " "))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, null)),
                () -> assertTrue(servicio.consultarHistorial(ADMINISTRATIVO, 1).isEmpty()));
    }

    @Test
    @DisplayName("Una solicitud o una regla inexistente lanza NoSuchElementException")
    void inexistentes() {
        assertAll(
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 99, cambio(Prioridad.URGENTE, 1, "M"))),
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.URGENTE, 99, "M"))),
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.consultarHistorial(ADMINISTRATIVO, 99)));
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "PERSONAL_ADMINISTRATIVO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Un usuario que no es personal administrativo recibe un error de permiso")
    void sinPermiso(Rol rol) {
        // Preparar
        Usuario otro = new Usuario("otro", rol);
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(SecurityException.class, () -> servicio.consultarPendientes(otro, null)),
                () -> assertThrows(SecurityException.class, () -> servicio.consultarReglasActivas(otro)),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.cambiarPrioridad(otro, 1, cambio(Prioridad.URGENTE, 1, "M"))),
                () -> assertThrows(SecurityException.class, () -> servicio.consultarHistorial(otro, 1)),
                () -> assertThrows(SecurityException.class, () -> servicio.consultarPendientes(null, null)));
    }

    @Test
    @DisplayName("Los datos de ejemplo tienen solicitudes pendientes y reglas activas")
    void datosDeEjemplo() {
        PrioridadesServiceSimulado ejemplo = PrioridadesServiceSimulado.conDatosDeEjemplo();
        assertAll(
                () -> assertEquals(4, ejemplo.consultarPendientes(ADMINISTRATIVO, null).size()),
                () -> assertEquals(3, ejemplo.consultarReglasActivas(ADMINISTRATIVO).size()));
    }
}
