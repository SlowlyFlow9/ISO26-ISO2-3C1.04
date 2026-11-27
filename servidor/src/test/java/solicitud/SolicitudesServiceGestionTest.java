package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import paciente.PacientesRepositorioMemoria;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import tipoprueba.RepositorioTiposPruebaMemoria;

/** CU13 - Servidor: consulta, modificación y anulación de solicitudes (D-031 y D-036). */
class SolicitudesServiceGestionTest {

    private static final Usuario MEDICO = new Usuario("medico", Rol.MEDICO);
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 10, 9, 0);

    private SolicitudesRepositorioMemoria solicitudes;
    private SolicitudesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        solicitudes = new SolicitudesRepositorioMemoria();
        servicio = new SolicitudesServiceImpl(
                new PacientesRepositorioMemoria(), new RepositorioTiposPruebaMemoria(), solicitudes);

        solicitudes.guardar(solicitud(1, 10, Prioridad.ORDINARIA, FECHA, EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(solicitud(2, 10, Prioridad.URGENTE, FECHA.plusDays(1), EstadoSolicitud.CITADA));
        solicitudes.guardar(solicitud(3, 20, Prioridad.URGENTE, FECHA, EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(new SolicitudPrueba(4, 20, 7, 1, "Control", "Seguimiento", Prioridad.PREFERENTE,
                FECHA, EstadoSolicitud.PENDIENTE_CITACION, 2, UnidadPeriodicidad.SEMANAS, 3, 1L));
        solicitudes.guardar(solicitud(5, 30, Prioridad.ORDINARIA, FECHA, EstadoSolicitud.REALIZADA));
    }

    private static SolicitudPrueba solicitud(long id, long paciente, Prioridad prioridad,
            LocalDateTime fecha, EstadoSolicitud estado) {
        return new SolicitudPrueba(id, paciente, 7, 1, "Dolor", "Descartar", prioridad, fecha, estado);
    }

    private static DatosModificacionSolicitud datos() {
        return new DatosModificacionSolicitud(" Dolor intenso ", " Revisión ", Prioridad.URGENTE, null, null, null);
    }

    private static List<Long> ids(List<SolicitudPrueba> lista) {
        return lista.stream().map(SolicitudPrueba::getId).toList();
    }

    // ---- Consulta ----

    @Test
    @DisplayName("Sin filtro se devuelven todas las solicitudes ordenadas por prioridad, fecha e id")
    void sinFiltroDevuelveTodasOrdenadas() throws SinPermisoException {
        // Ejecutar
        List<SolicitudPrueba> lista = servicio.consultarSolicitudes(MEDICO, null);
        // Comprobar
        assertEquals(List.of(3L, 2L, 4L, 1L, 5L), ids(lista));
    }

    @Test
    @DisplayName("Los filtros por paciente, estado y prioridad se pueden combinar")
    void filtrosCombinados() throws SinPermisoException {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(List.of(2L, 1L),
                        ids(servicio.consultarSolicitudes(MEDICO, new FiltroSolicitudes(10L, null, null)))),
                () -> assertEquals(List.of(3L, 4L, 1L), ids(servicio.consultarSolicitudes(MEDICO,
                        new FiltroSolicitudes(null, EstadoSolicitud.PENDIENTE_CITACION, null)))),
                () -> assertEquals(List.of(3L), ids(servicio.consultarSolicitudes(MEDICO,
                        new FiltroSolicitudes(20L, EstadoSolicitud.PENDIENTE_CITACION, Prioridad.URGENTE)))),
                () -> assertTrue(servicio.consultarSolicitudes(MEDICO,
                        new FiltroSolicitudes(null, EstadoSolicitud.ANULADA, null)).isEmpty()));
    }

    // ---- Modificación ----

    @Test
    @DisplayName("Se modifican los datos clínicos de una solicitud pendiente y el resto se conserva")
    void modificarSolicitudPendiente() throws SinPermisoException {
        // Ejecutar
        SolicitudPrueba modificada = servicio.modificarSolicitud(MEDICO, 1, datos());
        // Comprobar
        assertAll(
                () -> assertEquals("Dolor intenso", modificada.getIndicacionClinica()),
                () -> assertEquals("Revisión", modificada.getJustificacion()),
                () -> assertEquals(Prioridad.URGENTE, modificada.getPrioridad()),
                () -> assertEquals(10, modificada.getPacienteId()),
                () -> assertEquals(7, modificada.getMedicoId()),
                () -> assertEquals(FECHA, modificada.getFechaSolicitud()),
                () -> assertEquals(EstadoSolicitud.PENDIENTE_CITACION, modificada.getEstado()),
                () -> assertFalse(modificada.esSeguimiento()),
                () -> assertEquals("Dolor intenso", servicio.consultarSolicitud(1).orElseThrow().getIndicacionClinica()));
    }

    @Test
    @DisplayName("Se modifica la periodicidad de un seguimiento y se conserva la prescripción original")
    void modificarPeriodicidadDeSeguimiento() throws SinPermisoException {
        // Preparar
        DatosModificacionSolicitud cambios = new DatosModificacionSolicitud(
                "Control", "Seguimiento", Prioridad.PREFERENTE, 1, UnidadPeriodicidad.MESES, 6);
        // Ejecutar
        SolicitudPrueba modificada = servicio.modificarSolicitud(MEDICO, 4, cambios);
        // Comprobar
        assertAll(
                () -> assertEquals(1, modificada.getIntervalo()),
                () -> assertEquals(UnidadPeriodicidad.MESES, modificada.getUnidad()),
                () -> assertEquals(6, modificada.getNumeroRepeticiones()),
                () -> assertEquals(1L, modificada.getSolicitudOrigenId()));
    }

    @Test
    @DisplayName("Una periodicidad incompleta o con valores menores que 1 se rechaza")
    void periodicidadNoValidaSeRechaza() {
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 4,
                        new DatosModificacionSolicitud("C", "S", Prioridad.ORDINARIA, 1, null, 3))),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 4,
                        new DatosModificacionSolicitud("C", "S", Prioridad.ORDINARIA, 0, UnidadPeriodicidad.DIAS, 3))),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 4,
                        new DatosModificacionSolicitud("C", "S", Prioridad.ORDINARIA, 1, UnidadPeriodicidad.DIAS, 0))));
    }

    @Test
    @DisplayName("Indicar periodicidad en una solicitud que no es un seguimiento se rechaza")
    void periodicidadEnSolicitudNormalSeRechaza() {
        // Preparar
        DatosModificacionSolicitud cambios = new DatosModificacionSolicitud(
                "Dolor", "Descartar", Prioridad.ORDINARIA, null, UnidadPeriodicidad.DIAS, null);
        // Ejecutar y comprobar
        assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 1, cambios));
    }

    @Test
    @DisplayName("Una modificación sin datos obligatorios se rechaza y la solicitud no cambia")
    void modificacionSinDatosObligatorios() {
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 1, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 1,
                        new DatosModificacionSolicitud(" ", "J", Prioridad.URGENTE, null, null, null))),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 1,
                        new DatosModificacionSolicitud("I", null, Prioridad.URGENTE, null, null, null))),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 1,
                        new DatosModificacionSolicitud("I", "J", null, null, null, null))),
                () -> assertEquals("Dolor", servicio.consultarSolicitud(1).orElseThrow().getIndicacionClinica()));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = "PENDIENTE_CITACION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("No se puede modificar ni anular una solicitud citada, realizada o anulada")
    void soloSeGestionaUnaSolicitudPendiente(EstadoSolicitud estado) {
        // Preparar
        solicitudes.guardar(solicitud(9, 40, Prioridad.ORDINARIA, FECHA, estado));
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(SolicitudNoGestionableException.class,
                        () -> servicio.modificarSolicitud(MEDICO, 9, datos())),
                () -> assertThrows(SolicitudNoGestionableException.class,
                        () -> servicio.anularSolicitud(MEDICO, 9, "Motivo")),
                () -> assertEquals(estado, servicio.consultarSolicitud(9).orElseThrow().getEstado()));
    }

    // ---- Anulación ----

    @Test
    @DisplayName("Anular una solicitud pendiente la pasa a ANULADA con su motivo y fecha")
    void anularSolicitudPendiente() throws SinPermisoException {
        // Ejecutar
        SolicitudPrueba anulada = servicio.anularSolicitud(MEDICO, 4, "  Ya no es necesaria  ");
        // Comprobar
        assertAll(
                () -> assertEquals(EstadoSolicitud.ANULADA, anulada.getEstado()),
                () -> assertEquals("Ya no es necesaria", anulada.getMotivoAnulacion()),
                () -> assertNotNull(anulada.getFechaAnulacion()),
                () -> assertTrue(anulada.esSeguimiento()),
                () -> assertFalse(anulada.sePuedeGestionar()),
                () -> assertEquals(EstadoSolicitud.ANULADA, servicio.consultarSolicitud(4).orElseThrow().getEstado()));
    }

    @Test
    @DisplayName("Una anulación sin motivo se rechaza")
    void anulacionSinMotivo() {
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.anularSolicitud(MEDICO, 1, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.anularSolicitud(MEDICO, 1, "  ")),
                () -> assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                        servicio.consultarSolicitud(1).orElseThrow().getEstado()));
    }

    @Test
    @DisplayName("Una solicitud que no está anulada no tiene datos de anulación")
    void solicitudSinAnularNoTieneDatosDeAnulacion() {
        // Ejecutar
        SolicitudPrueba solicitud = servicio.consultarSolicitud(1).orElseThrow();
        // Comprobar
        assertAll(
                () -> assertNull(solicitud.getMotivoAnulacion()),
                () -> assertNull(solicitud.getFechaAnulacion()),
                () -> assertTrue(solicitud.sePuedeGestionar()));
    }

    // ---- Errores comunes ----

    @Test
    @DisplayName("Modificar o anular una solicitud inexistente lanza NoSuchElementException")
    void solicitudInexistente() {
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.modificarSolicitud(MEDICO, 99, datos())),
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.anularSolicitud(MEDICO, 99, "Motivo")));
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "MEDICO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Un usuario sin rol de Médico no puede gestionar solicitudes")
    void sinRolMedico(Rol rol) {
        // Preparar
        Usuario otro = new Usuario("otro", rol);
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(SinPermisoException.class,
                        () -> servicio.consultarSolicitudes(otro, FiltroSolicitudes.vacio())),
                () -> assertThrows(SinPermisoException.class, () -> servicio.modificarSolicitud(otro, 1, datos())),
                () -> assertThrows(SinPermisoException.class, () -> servicio.anularSolicitud(otro, 1, "Motivo")),
                () -> assertThrows(SinPermisoException.class,
                        () -> servicio.consultarSolicitudes(null, FiltroSolicitudes.vacio())));
    }

    @Test
    @DisplayName("El repositorio devuelve todas las solicitudes guardadas")
    void repositorioDevuelveTodas() {
        // Ejecutar y comprobar
        assertEquals(5, solicitudes.todas().size());
    }

    @ParameterizedTest
    @EnumSource(EstadoSolicitud.class)
    @DisplayName("Solo el estado PENDIENTE_CITACION permite gestionar la solicitud")
    void estadosQuePermitenGestion(EstadoSolicitud estado) {
        // Comprobar
        assertEquals(estado == EstadoSolicitud.PENDIENTE_CITACION, estado.permiteGestion());
    }
}
