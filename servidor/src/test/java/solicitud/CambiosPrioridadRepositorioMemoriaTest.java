package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU28 - Servidor: historial de cambios de prioridad en memoria y copia de la solicitud. */
class CambiosPrioridadRepositorioMemoriaTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 28, 10, 0);

    private CambiosPrioridadRepositorioMemoria repositorio;

    @BeforeEach
    void setUp() {
        repositorio = new CambiosPrioridadRepositorioMemoria();
    }

    private static CambioPrioridad cambio(long id, long solicitudId, Prioridad nueva) {
        return new CambioPrioridad(id, solicitudId, Prioridad.ORDINARIA, nueva, 1, "Motivo", "admin", FECHA);
    }

    @Test
    @DisplayName("Un cambio con id 0 recibe un id nuevo y consecutivo")
    void asignaIds() {
        // Ejecutar
        CambioPrioridad primero = repositorio.guardar(cambio(0, 1, Prioridad.URGENTE));
        CambioPrioridad segundo = repositorio.guardar(cambio(0, 1, Prioridad.PREFERENTE));
        // Comprobar
        assertAll(
                () -> assertEquals(1, primero.id()),
                () -> assertEquals(2, segundo.id()),
                () -> assertEquals(Prioridad.URGENTE, primero.prioridadNueva()));
    }

    @Test
    @DisplayName("Un cambio con id conserva su id y los siguientes continúan después")
    void conservaIdIndicado() {
        // Ejecutar
        repositorio.guardar(cambio(7, 1, Prioridad.URGENTE));
        CambioPrioridad siguiente = repositorio.guardar(cambio(0, 1, Prioridad.PREFERENTE));
        // Comprobar
        assertEquals(8, siguiente.id());
    }

    @Test
    @DisplayName("El historial de una solicitud va del cambio más reciente al más antiguo y no mezcla solicitudes")
    void historialPorSolicitud() {
        // Preparar
        repositorio.guardar(cambio(0, 1, Prioridad.PREFERENTE));
        repositorio.guardar(cambio(0, 2, Prioridad.URGENTE));
        repositorio.guardar(cambio(0, 1, Prioridad.URGENTE));
        // Ejecutar
        List<CambioPrioridad> historial = repositorio.listarPorSolicitud(1);
        // Comprobar
        assertAll(
                () -> assertEquals(List.of(3L, 1L), historial.stream().map(CambioPrioridad::id).toList()),
                () -> assertTrue(repositorio.listarPorSolicitud(99).isEmpty()));
    }

    @Test
    @DisplayName("Un cambio nulo o con id negativo se rechaza")
    void cambiosNoValidos() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> repositorio.guardar(null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> repositorio.guardar(cambio(-1, 1, Prioridad.URGENTE))));
    }

    @Test
    @DisplayName("conPrioridad cambia solo la prioridad de la solicitud")
    void conPrioridadConservaElResto() {
        // Preparar
        SolicitudPrueba original = new SolicitudPrueba(5, 10, 7, 3, "Dolor", "Descartar",
                Prioridad.ORDINARIA, FECHA, EstadoSolicitud.PENDIENTE_CITACION,
                2, UnidadPeriodicidad.SEMANAS, 3, 1L);
        // Ejecutar
        SolicitudPrueba cambiada = original.conPrioridad(Prioridad.URGENTE);
        // Comprobar
        assertAll(
                () -> assertEquals(Prioridad.URGENTE, cambiada.getPrioridad()),
                () -> assertEquals(Prioridad.ORDINARIA, original.getPrioridad()),
                () -> assertEquals(5, cambiada.getId()),
                () -> assertEquals("Dolor", cambiada.getIndicacionClinica()),
                () -> assertEquals(EstadoSolicitud.PENDIENTE_CITACION, cambiada.getEstado()),
                () -> assertEquals(2, cambiada.getIntervalo()),
                () -> assertEquals(1L, cambiada.getSolicitudOrigenId()),
                () -> assertEquals(FECHA, cambiada.getFechaSolicitud()));
    }

    @Test
    @DisplayName("Las vistas de solicitud y regla copian solo los datos necesarios")
    void vistasDeSolicitudYRegla() {
        // Preparar
        SolicitudPrueba solicitud = new SolicitudPrueba(5, 10, 7, 3, "Dolor", "Descartar",
                Prioridad.PREFERENTE, FECHA, EstadoSolicitud.PENDIENTE_CITACION);
        ReglaPrioridad regla = new ReglaPrioridad(4, "Oncológico", "Tratamiento activo", 2, true, "root", FECHA);
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(new SolicitudPrioridad(5, 10, 3, Prioridad.PREFERENTE, FECHA),
                        SolicitudPrioridad.de(solicitud)),
                () -> assertEquals(new ReglaPrioridadAplicable(4, "Oncológico", "Tratamiento activo", 2),
                        ReglaPrioridadAplicable.de(regla)));
    }
}
