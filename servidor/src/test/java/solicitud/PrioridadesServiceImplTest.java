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
import paciente.SinPermisoException;
import paciente.Usuario;

/** CU28 - Servidor: reglas de la gestión de prioridades (D-038). */
class PrioridadesServiceImplTest {

    private static final Usuario ADMINISTRATIVO = new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 20, 9, 0);

    private SolicitudesRepositorioMemoria solicitudes;
    private ReglasPrioridadRepositorioMemoria reglas;
    private CambiosPrioridadRepositorioMemoria cambios;
    private PrioridadesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        solicitudes = new SolicitudesRepositorioMemoria();
        reglas = new ReglasPrioridadRepositorioMemoria();
        cambios = new CambiosPrioridadRepositorioMemoria();
        servicio = new PrioridadesServiceImpl(solicitudes, reglas, cambios);

        solicitudes.guardar(solicitud(1, Prioridad.ORDINARIA, FECHA, EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(solicitud(2, Prioridad.URGENTE, FECHA.plusDays(1), EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(solicitud(3, Prioridad.URGENTE, FECHA, EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(solicitud(4, Prioridad.PREFERENTE, FECHA, EstadoSolicitud.CITADA));

        reglas.guardar(new ReglaPrioridad(1, "Oncológico", "Tratamiento activo", 2, true, "root", FECHA));
        reglas.guardar(new ReglaPrioridad(2, "Espera prolongada", "Más de 60 días", 1, true, "root", FECHA));
        reglas.guardar(new ReglaPrioridad(3, "Retirada", "Ya no se aplica", 3, false, "root", FECHA));
    }

    private static SolicitudPrueba solicitud(long id, Prioridad prioridad, LocalDateTime fecha,
            EstadoSolicitud estado) {
        return new SolicitudPrueba(id, 10 * id, 7, 1, "Dolor", "Descartar", prioridad, fecha, estado);
    }

    private static DatosCambioPrioridad cambio(Prioridad prioridad, Long regla, String motivo) {
        return new DatosCambioPrioridad(prioridad, regla, motivo);
    }

    private static List<Long> ids(List<SolicitudPrioridad> lista) {
        return lista.stream().map(SolicitudPrioridad::solicitudId).toList();
    }

    // ---- Consultas ----

    @Test
    @DisplayName("Solo se listan las solicitudes pendientes, ordenadas por prioridad, fecha e id")
    void pendientesOrdenadas() throws SinPermisoException {
        assertEquals(List.of(3L, 2L, 1L), ids(servicio.consultarPendientes(ADMINISTRATIVO, null)));
    }

    @Test
    @DisplayName("El filtro por prioridad devuelve solo las pendientes con esa prioridad")
    void filtroPorPrioridad() throws SinPermisoException {
        assertAll(
                () -> assertEquals(List.of(3L, 2L), ids(servicio.consultarPendientes(ADMINISTRATIVO, Prioridad.URGENTE))),
                () -> assertTrue(servicio.consultarPendientes(ADMINISTRATIVO, Prioridad.PREFERENTE).isEmpty()));
    }

    @Test
    @DisplayName("La lista no incluye datos clínicos, solo los necesarios")
    void listaSinDatosClinicos() throws SinPermisoException {
        assertEquals(new SolicitudPrioridad(1, 10, 1, Prioridad.ORDINARIA, FECHA),
                servicio.consultarPendientes(ADMINISTRATIVO, Prioridad.ORDINARIA).get(0));
    }

    @Test
    @DisplayName("Solo se devuelven las reglas activas de CU37, ordenadas por su orden")
    void reglasActivas() throws SinPermisoException {
        assertEquals(List.of(2L, 1L), servicio.consultarReglasActivas(ADMINISTRATIVO).stream()
                .map(ReglaPrioridadAplicable::id).toList());
    }

    // ---- Cambio de prioridad ----

    @Test
    @DisplayName("Cambiar la prioridad la guarda, conserva el resto y reordena la lista")
    void cambiarPrioridad() throws SinPermisoException {
        // Ejecutar
        SolicitudPrioridad actualizada = servicio.cambiarPrioridad(ADMINISTRATIVO, 1,
                cambio(Prioridad.URGENTE, 1L, "  Empeoramiento  "));
        // Comprobar
        SolicitudPrueba guardada = solicitudes.buscarPorId(1).orElseThrow();
        assertAll(
                () -> assertEquals(Prioridad.URGENTE, actualizada.prioridad()),
                () -> assertEquals(Prioridad.URGENTE, guardada.getPrioridad()),
                () -> assertEquals("Dolor", guardada.getIndicacionClinica()),
                () -> assertEquals(EstadoSolicitud.PENDIENTE_CITACION, guardada.getEstado()),
                () -> assertEquals(FECHA, guardada.getFechaSolicitud()),
                () -> assertEquals(List.of(1L, 3L, 2L), ids(servicio.consultarPendientes(ADMINISTRATIVO, null))));
    }

    @Test
    @DisplayName("Cada cambio se guarda en el historial con la regla, el motivo, el usuario y la fecha")
    void historial() throws SinPermisoException {
        // Preparar
        servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.PREFERENTE, 2L, "Espera"));
        servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.URGENTE, 1L, " Empeoramiento "));
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
                () -> assertEquals(Prioridad.ORDINARIA, historial.get(1).prioridadAnterior()),
                () -> assertTrue(servicio.consultarHistorial(ADMINISTRATIVO, 2).isEmpty()));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = "PENDIENTE_CITACION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("No se puede cambiar la prioridad de una solicitud citada, realizada o anulada")
    void soloPendientes(EstadoSolicitud estado) {
        // Preparar
        solicitudes.guardar(solicitud(9, Prioridad.ORDINARIA, FECHA, estado));
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(SolicitudNoGestionableException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 9, cambio(Prioridad.URGENTE, 1L, "M"))),
                () -> assertEquals(Prioridad.ORDINARIA, solicitudes.buscarPorId(9).orElseThrow().getPrioridad()));
    }

    @Test
    @DisplayName("Sin prioridad, sin regla o sin motivo se rechaza y no se guarda nada")
    void datosObligatorios() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(null, 1L, "M"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.URGENTE, null, "M"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.URGENTE, 1L, "  "))),
                () -> assertTrue(cambios.listarPorSolicitud(1).isEmpty()));
    }

    @Test
    @DisplayName("Una regla retirada o la misma prioridad se rechazan")
    void reglaRetiradaYMismaPrioridad() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.URGENTE, 3L, "M"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.ORDINARIA, 1L, "M"))),
                () -> assertTrue(cambios.listarPorSolicitud(1).isEmpty()));
    }

    @Test
    @DisplayName("Una solicitud o una regla inexistente lanza NoSuchElementException")
    void inexistentes() {
        assertAll(
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 99, cambio(Prioridad.URGENTE, 1L, "M"))),
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 1, cambio(Prioridad.URGENTE, 99L, "M"))),
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.consultarHistorial(ADMINISTRATIVO, 99)));
    }

    // ---- Permisos ----

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "PERSONAL_ADMINISTRATIVO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Un usuario que no es personal administrativo recibe un error de permiso")
    void sinPermiso(Rol rol) {
        // Preparar
        Usuario otro = new Usuario("otro", rol);
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(SinPermisoException.class, () -> servicio.consultarPendientes(otro, null)),
                () -> assertThrows(SinPermisoException.class, () -> servicio.consultarReglasActivas(otro)),
                () -> assertThrows(SinPermisoException.class,
                        () -> servicio.cambiarPrioridad(otro, 1, cambio(Prioridad.URGENTE, 1L, "M"))),
                () -> assertThrows(SinPermisoException.class, () -> servicio.consultarHistorial(otro, 1)),
                () -> assertThrows(SinPermisoException.class, () -> servicio.consultarPendientes(null, null)),
                () -> assertEquals(Prioridad.ORDINARIA, solicitudes.buscarPorId(1).orElseThrow().getPrioridad()));
    }

    @Test
    @DisplayName("El servicio exige sus tres repositorios")
    void repositoriosObligatorios() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PrioridadesServiceImpl(null, reglas, cambios)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PrioridadesServiceImpl(solicitudes, null, cambios)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PrioridadesServiceImpl(solicitudes, reglas, null)));
    }
}
