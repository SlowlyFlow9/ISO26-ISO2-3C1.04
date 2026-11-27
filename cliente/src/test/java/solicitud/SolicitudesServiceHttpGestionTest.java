package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import paciente.Rol;
import paciente.Usuario;

/** CU13: el cliente HTTP usa las rutas de la gestión de solicitudes (D-037). */
class SolicitudesServiceHttpGestionTest {
    private static final Usuario MEDICO = new Usuario("medico", Rol.MEDICO);

    private HttpServer servidor;
    private SolicitudesServiceHttp servicio;
    private volatile int estado;
    private volatile String respuesta;
    private volatile String metodoRecibido;
    private volatile String rutaRecibida;
    private volatile String consultaRecibida;
    private volatile String cuerpoRecibido;

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", exchange -> {
            metodoRecibido = exchange.getRequestMethod();
            rutaRecibida = exchange.getRequestURI().getPath();
            consultaRecibida = exchange.getRequestURI().getQuery();
            cuerpoRecibido = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        servidor.start();
        servicio = new SolicitudesServiceHttp("http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void tearDown() { servidor.stop(0); }

    private static String json(long id, String estado, String extra) {
        return "{\"id\":" + id + ",\"pacienteId\":1,\"medicoId\":2,\"tipoPruebaId\":3,"
                + "\"indicacionClinica\":\"Dolor\",\"justificacion\":\"Control\",\"prioridad\":\"URGENTE\","
                + "\"fechaSolicitud\":\"2026-11-14T10:30:00\",\"estado\":\"" + estado + "\"" + extra + "}";
    }

    private static DatosModificacionSolicitud datos() {
        return new DatosModificacionSolicitud("Dolor", "Control", Prioridad.URGENTE, null, null, null);
    }

    // ---- Listado ----

    @Test
    @DisplayName("Consultar sin filtros hace GET /solicitudes sin parámetros")
    void consultarSinFiltros() {
        // Preparar
        estado = 200;
        respuesta = "[" + json(1, "PENDIENTE_CITACION", "") + "," + json(2, "CITADA", "") + "]";
        // Ejecutar
        List<SolicitudPrueba> lista = servicio.consultarSolicitudes(MEDICO, FiltroSolicitudes.vacio());
        // Comprobar
        assertAll(
                () -> assertEquals("GET", metodoRecibido),
                () -> assertEquals("/solicitudes", rutaRecibida),
                () -> assertNull(consultaRecibida),
                () -> assertEquals(2, lista.size()),
                () -> assertEquals(EstadoSolicitud.CITADA, lista.get(1).getEstado()));
    }

    @Test
    @DisplayName("Consultar con filtros envía paciente, estado y prioridad como parámetros")
    void consultarConFiltros() {
        // Preparar
        estado = 200;
        respuesta = "[]";
        FiltroSolicitudes filtro = new FiltroSolicitudes(5L, EstadoSolicitud.PENDIENTE_CITACION, Prioridad.URGENTE);
        // Ejecutar
        List<SolicitudPrueba> lista = servicio.consultarSolicitudes(MEDICO, filtro);
        // Comprobar
        assertAll(
                () -> assertEquals("pacienteId=5&estado=PENDIENTE_CITACION&prioridad=URGENTE", consultaRecibida),
                () -> assertTrue(lista.isEmpty()));
    }

    @Test
    @DisplayName("Un filtro nulo equivale a no filtrar")
    void filtroNuloNoFiltra() {
        // Preparar
        estado = 200;
        respuesta = "[]";
        // Ejecutar
        servicio.consultarSolicitudes(MEDICO, null);
        // Comprobar
        assertNull(consultaRecibida);
    }

    // ---- Modificación ----

    @Test
    @DisplayName("Modificar hace PUT /solicitudes/{id} con los datos clínicos")
    void modificarEnviaPut() {
        // Preparar
        estado = 200;
        respuesta = json(7, "PENDIENTE_CITACION", "");
        // Ejecutar
        SolicitudPrueba modificada = servicio.modificarSolicitud(MEDICO, 7, datos());
        // Comprobar
        assertAll(
                () -> assertEquals("PUT", metodoRecibido),
                () -> assertEquals("/solicitudes/7", rutaRecibida),
                () -> assertTrue(cuerpoRecibido.contains("\"indicacionClinica\":\"Dolor\"")),
                () -> assertTrue(cuerpoRecibido.contains("\"prioridad\":\"URGENTE\"")),
                () -> assertEquals(7, modificada.getId()));
    }

    @Test
    @DisplayName("Modificar una solicitud que no está pendiente (409) lanza IllegalStateException")
    void modificarConflictoLanzaIllegalState() {
        // Preparar
        estado = 409;
        respuesta = "{\"error\":\"ESTADO_NO_PERMITIDO\"}";
        // Ejecutar
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.modificarSolicitud(MEDICO, 7, datos()));
        // Comprobar
        assertTrue(error.getMessage().contains("pendiente de citacion"));
    }

    @Test
    @DisplayName("Modificar una solicitud inexistente (404) lanza NoSuchElementException")
    void modificarInexistenteLanzaNoSuchElement() {
        // Preparar
        estado = 404;
        respuesta = "{}";
        // Ejecutar
        NoSuchElementException error = assertThrows(NoSuchElementException.class,
                () -> servicio.modificarSolicitud(MEDICO, 99, datos()));
        // Comprobar
        assertEquals("No se ha encontrado la solicitud.", error.getMessage());
    }

    @Test
    @DisplayName("Modificar con datos no válidos (400) lanza IllegalArgumentException")
    void modificarDatosNoValidos() {
        // Preparar
        estado = 400;
        respuesta = "{}";
        // Ejecutar y comprobar
        assertThrows(IllegalArgumentException.class, () -> servicio.modificarSolicitud(MEDICO, 7, datos()));
    }

    // ---- Anulación ----

    @Test
    @DisplayName("Anular hace POST /solicitudes/{id}/anulacion con el motivo")
    void anularEnviaPost() {
        // Preparar
        estado = 200;
        respuesta = json(7, "ANULADA",
                ",\"motivoAnulacion\":\"Duplicada\",\"fechaAnulacion\":\"2026-11-18T12:00:00\"");
        // Ejecutar
        SolicitudPrueba anulada = servicio.anularSolicitud(MEDICO, 7, " Duplicada ");
        // Comprobar
        assertAll(
                () -> assertEquals("POST", metodoRecibido),
                () -> assertEquals("/solicitudes/7/anulacion", rutaRecibida),
                () -> assertEquals("{\"motivo\":\"Duplicada\"}", cuerpoRecibido),
                () -> assertEquals(EstadoSolicitud.ANULADA, anulada.getEstado()),
                () -> assertEquals("Duplicada", anulada.getMotivoAnulacion()));
    }

    @Test
    @DisplayName("Anular sin motivo no llega a enviar la petición")
    void anularSinMotivoNoEnvia() {
        // Ejecutar
        assertThrows(IllegalArgumentException.class, () -> servicio.anularSolicitud(MEDICO, 7, "  "));
        // Comprobar
        assertNull(rutaRecibida);
    }

    @Test
    @DisplayName("Anular una solicitud ya citada (409) lanza IllegalStateException")
    void anularConflictoLanzaIllegalState() {
        // Preparar
        estado = 409;
        respuesta = "{}";
        // Ejecutar y comprobar
        assertThrows(IllegalStateException.class, () -> servicio.anularSolicitud(MEDICO, 7, "Motivo"));
    }

    @Test
    @DisplayName("Un usuario sin permiso (403) recibe SecurityException en todas las operaciones")
    void sinPermisoLanzaSecurityException() {
        // Preparar
        estado = 403;
        respuesta = "{}";
        // Ejecutar y comprobar
        assertAll(
                () -> assertThrows(SecurityException.class,
                        () -> servicio.consultarSolicitudes(MEDICO, FiltroSolicitudes.vacio())),
                () -> assertThrows(SecurityException.class, () -> servicio.modificarSolicitud(MEDICO, 7, datos())),
                () -> assertThrows(SecurityException.class, () -> servicio.anularSolicitud(MEDICO, 7, "Motivo")));
    }
}
