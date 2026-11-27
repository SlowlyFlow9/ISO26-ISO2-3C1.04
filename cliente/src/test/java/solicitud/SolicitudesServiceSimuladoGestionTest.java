package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

/** CU13 - Cliente: reglas de la gestión de solicitudes en el servicio simulado (D-031 y D-036). */
class SolicitudesServiceSimuladoGestionTest {

    private static final Usuario MEDICO = new Usuario("medico", Rol.MEDICO);
    private static final Usuario ADMINISTRATIVO = new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 10, 9, 0);

    private SolicitudesServiceSimulado servicio;

    @BeforeEach
    void setUp() {
        servicio = new SolicitudesServiceSimulado(List.of(), List.of(
                solicitud(1, 10, Prioridad.ORDINARIA, FECHA, EstadoSolicitud.PENDIENTE_CITACION),
                solicitud(2, 10, Prioridad.URGENTE, FECHA.plusDays(1), EstadoSolicitud.CITADA),
                solicitud(3, 20, Prioridad.URGENTE, FECHA, EstadoSolicitud.PENDIENTE_CITACION),
                new SolicitudPrueba(4, 20, 7, 1, "Control", "Seguimiento", Prioridad.PREFERENTE, FECHA,
                        EstadoSolicitud.PENDIENTE_CITACION, 2, UnidadPeriodicidad.SEMANAS, 3, 1L)));
    }

    private static SolicitudPrueba solicitud(long id, long paciente, Prioridad prioridad,
            LocalDateTime fecha, EstadoSolicitud estado) {
        return new SolicitudPrueba(id, paciente, 7, 1, "Dolor", "Descartar", prioridad, fecha, estado);
    }

    private static DatosModificacionSolicitud datosSinPeriodicidad() {
        return new DatosModificacionSolicitud("Dolor intenso", "Revisión", Prioridad.URGENTE, null, null, null);
    }

    // ---- Consulta ----

    @Test
    @DisplayName("Sin filtros se devuelven todas las solicitudes ordenadas por prioridad y fecha")
    void sinFiltroDevuelveTodasOrdenadas() {
        // Ejecutar
        List<SolicitudPrueba> lista = servicio.consultarSolicitudes(MEDICO, FiltroSolicitudes.vacio());
        // Comprobar
        assertEquals(List.of(3L, 2L, 4L, 1L), lista.stream().map(SolicitudPrueba::getId).toList());
    }

    @Test
    @DisplayName("Los filtros por paciente, estado y prioridad se combinan")
    void filtrosCombinados() {
        // Preparar
        FiltroSolicitudes filtro = new FiltroSolicitudes(10L, EstadoSolicitud.PENDIENTE_CITACION, null);
        // Ejecutar
        List<SolicitudPrueba> lista = servicio.consultarSolicitudes(MEDICO, filtro);
        // Comprobar
        assertEquals(List.of(1L), lista.stream().map(SolicitudPrueba::getId).toList());
    }

    @Test
    @DisplayName("Un filtro que no cumple ninguna solicitud devuelve la lista vacía")
    void filtroSinResultados() {
        // Ejecutar
        List<SolicitudPrueba> lista = servicio.consultarSolicitudes(MEDICO,
                new FiltroSolicitudes(null, EstadoSolicitud.ANULADA, null));
        // Comprobar
        assertEquals(0, lista.size());
    }

    // ---- Modificación ----

    @Test
    @DisplayName("Se modifican los datos clínicos de una solicitud pendiente y se conserva el resto")
    void modificarSolicitudPendiente() {
        // Ejecutar
        SolicitudPrueba modificada = servicio.modificarSolicitud(MEDICO, 1, datosSinPeriodicidad());
        // Comprobar
        assertAll(
                () -> assertEquals("Dolor intenso", modificada.getIndicacionClinica()),
                () -> assertEquals("Revisión", modificada.getJustificacion()),
                () -> assertEquals(Prioridad.URGENTE, modificada.getPrioridad()),
                () -> assertEquals(10, modificada.getPacienteId()),
                () -> assertEquals(FECHA, modificada.getFechaSolicitud()),
                () -> assertEquals(EstadoSolicitud.PENDIENTE_CITACION, modificada.getEstado()),
                () -> assertEquals(modificada, servicio.consultarSolicitud(MEDICO, 1)));
    }

    @Test
    @DisplayName("Se modifica la periodicidad de un seguimiento")
    void modificarPeriodicidadDeSeguimiento() {
        // Preparar
        DatosModificacionSolicitud datos = new DatosModificacionSolicitud(
                "Control", "Seguimiento", Prioridad.PREFERENTE, 1, UnidadPeriodicidad.MESES, 6);
        // Ejecutar
        SolicitudPrueba modificada = servicio.modificarSolicitud(MEDICO, 4, datos);
        // Comprobar
        assertAll(
                () -> assertEquals(1, modificada.getIntervalo()),
                () -> assertEquals(UnidadPeriodicidad.MESES, modificada.getUnidad()),
                () -> assertEquals(6, modificada.getNumeroRepeticiones()),
                () -> assertEquals(1L, modificada.getSolicitudOrigenId()));
    }

    @Test
    @DisplayName("Una periodicidad incompleta en un seguimiento se rechaza")
    void periodicidadIncompletaSeRechaza() {
        // Preparar
        DatosModificacionSolicitud datos = new DatosModificacionSolicitud(
                "Control", "Seguimiento", Prioridad.PREFERENTE, 1, null, 6);
        // Ejecutar y comprobar
        assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 4, datos));
    }

    @Test
    @DisplayName("Indicar periodicidad en una solicitud que no es un seguimiento se rechaza")
    void periodicidadEnSolicitudNormalSeRechaza() {
        // Preparar
        DatosModificacionSolicitud datos = new DatosModificacionSolicitud(
                "Dolor", "Descartar", Prioridad.ORDINARIA, 1, UnidadPeriodicidad.DIAS, 2);
        // Ejecutar y comprobar
        assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 1, datos));
    }

    @Test
    @DisplayName("Una modificación sin datos obligatorios se rechaza y la solicitud no cambia")
    void modificacionSinDatosObligatorios() {
        // Preparar
        DatosModificacionSolicitud datos = new DatosModificacionSolicitud(" ", "Revisión", null, null, null, null);
        // Ejecutar
        assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 1, datos));
        // Comprobar
        assertEquals("Dolor", servicio.consultarSolicitud(MEDICO, 1).getIndicacionClinica());
    }

    @Test
    @DisplayName("Modificar una solicitud citada se rechaza")
    void modificarSolicitudCitadaSeRechaza() {
        // Ejecutar y comprobar
        assertThrows(IllegalStateException.class,
                () -> servicio.modificarSolicitud(MEDICO, 2, datosSinPeriodicidad()));
    }

    // ---- Anulación ----

    @Test
    @DisplayName("Anular una solicitud pendiente la pasa a ANULADA con su motivo y fecha")
    void anularSolicitudPendiente() {
        // Ejecutar
        SolicitudPrueba anulada = servicio.anularSolicitud(MEDICO, 3, "  Duplicada  ");
        // Comprobar
        assertAll(
                () -> assertEquals(EstadoSolicitud.ANULADA, anulada.getEstado()),
                () -> assertEquals("Duplicada", anulada.getMotivoAnulacion()),
                () -> assertNotNull(anulada.getFechaAnulacion()),
                () -> assertEquals(EstadoSolicitud.ANULADA, servicio.consultarSolicitud(MEDICO, 3).getEstado()));
    }

    @Test
    @DisplayName("Una anulación sin motivo se rechaza")
    void anulacionSinMotivoSeRechaza() {
        // Ejecutar y comprobar
        assertThrows(IllegalArgumentException.class, () -> servicio.anularSolicitud(MEDICO, 3, " "));
    }

    @Test
    @DisplayName("Una solicitud anulada ya no se puede modificar ni anular otra vez")
    void solicitudAnuladaNoSeGestiona() {
        // Preparar
        servicio.anularSolicitud(MEDICO, 3, "Duplicada");
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(IllegalStateException.class,
                        () -> servicio.anularSolicitud(MEDICO, 3, "Otra vez")),
                () -> assertThrows(IllegalStateException.class,
                        () -> servicio.modificarSolicitud(MEDICO, 3, datosSinPeriodicidad())));
    }

    // ---- Errores comunes ----

    @Test
    @DisplayName("Las operaciones sobre una solicitud inexistente lanzan NoSuchElementException")
    void solicitudInexistente() {
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(NoSuchElementException.class, () -> servicio.consultarSolicitud(MEDICO, 99)),
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.modificarSolicitud(MEDICO, 99, datosSinPeriodicidad())),
                () -> assertThrows(NoSuchElementException.class,
                        () -> servicio.anularSolicitud(MEDICO, 99, "Motivo")));
    }

    @Test
    @DisplayName("Un usuario sin rol de Médico recibe un error de permiso")
    void usuarioSinRolMedico() {
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(SecurityException.class,
                        () -> servicio.consultarSolicitudes(ADMINISTRATIVO, FiltroSolicitudes.vacio())),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.modificarSolicitud(ADMINISTRATIVO, 1, datosSinPeriodicidad())),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.anularSolicitud(ADMINISTRATIVO, 1, "Motivo")));
    }

    @Test
    @DisplayName("Una solicitud sin anular no tiene datos de anulación")
    void solicitudSinAnularNoTieneDatosDeAnulacion() {
        // Ejecutar
        SolicitudPrueba solicitud = servicio.consultarSolicitud(MEDICO, 1);
        // Comprobar
        assertAll(
                () -> assertNull(solicitud.getMotivoAnulacion()),
                () -> assertNull(solicitud.getFechaAnulacion()));
    }
}
