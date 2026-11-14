package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import paciente.Rol;
import paciente.Usuario;

/** CU12: el cliente HTTP envía y recibe la periodicidad de los seguimientos (D-030). */
class SolicitudesServiceHttpSeguimientoTest {
    private static final Usuario MEDICO = new Usuario("medico", Rol.MEDICO);

    private HttpServer servidor;
    private SolicitudesServiceHttp servicio;
    private volatile int estado;
    private volatile String respuesta;
    private volatile String cuerpoRecibido;
    private volatile String rutaRecibida;

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", exchange -> {
            rutaRecibida = exchange.getRequestURI().getPath();
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

    private SolicitudPrueba datosSeguimiento() {
        return new SolicitudPrueba(0, 1, 2, 3, "Dolor", "Control", Prioridad.PREFERENTE, null, null,
                7, UnidadPeriodicidad.SEMANAS, 3, 4L);
    }

    private SolicitudPrueba datosSinPeriodicidad() {
        return new SolicitudPrueba(0, 1, 2, 3, "Dolor", "Control", Prioridad.PREFERENTE, null, null);
    }

    private String json(String periodicidad) {
        return "{\"id\":5,\"pacienteId\":1,\"medicoId\":2,\"tipoPruebaId\":3,"
                + "\"indicacionClinica\":\"Dolor\",\"justificacion\":\"Control\",\"prioridad\":\"PREFERENTE\","
                + "\"fechaSolicitud\":\"2026-11-14T10:30:00\",\"estado\":\"PENDIENTE_CITACION\"" + periodicidad + "}";
    }

    private static final String PERIODICIDAD =
            ",\"intervalo\":7,\"unidad\":\"SEMANAS\",\"numeroRepeticiones\":3,\"solicitudOrigenId\":4";

    // ---- Envío ----

    @Test
    @DisplayName("Crear un seguimiento envía la periodicidad en el cuerpo JSON")
    void crearSeguimientoEnviaLaPeriodicidad() {
        // Preparar
        estado = 201;
        respuesta = json(PERIODICIDAD);
        // Ejecutar
        servicio.crearSolicitud(MEDICO, datosSeguimiento());
        // Comprobar
        assertAll(
                () -> assertEquals("/solicitudes", rutaRecibida),
                () -> assertTrue(cuerpoRecibido.contains("\"intervalo\":7")),
                () -> assertTrue(cuerpoRecibido.contains("\"unidad\":\"SEMANAS\"")),
                () -> assertTrue(cuerpoRecibido.contains("\"numeroRepeticiones\":3")),
                () -> assertTrue(cuerpoRecibido.contains("\"solicitudOrigenId\":4")));
    }

    @Test
    @DisplayName("Crear una solicitud sin periodicidad no envía los campos de seguimiento")
    void crearSinPeriodicidadNoEnviaCamposDeSeguimiento() {
        // Preparar
        estado = 201;
        respuesta = json("");
        // Ejecutar
        servicio.crearSolicitud(MEDICO, datosSinPeriodicidad());
        // Comprobar
        assertAll(
                () -> assertFalse(cuerpoRecibido.contains("intervalo")),
                () -> assertFalse(cuerpoRecibido.contains("unidad")),
                () -> assertFalse(cuerpoRecibido.contains("numeroRepeticiones")),
                () -> assertFalse(cuerpoRecibido.contains("solicitudOrigenId")));
    }

    // ---- Recepción ----

    @Test
    @DisplayName("La solicitud creada devuelve la periodicidad registrada")
    void crearSeguimientoDevuelveLaPeriodicidad() {
        // Preparar
        estado = 201;
        respuesta = json(PERIODICIDAD);
        // Ejecutar
        SolicitudPrueba creada = servicio.crearSolicitud(MEDICO, datosSeguimiento());
        // Comprobar
        assertAll(
                () -> assertTrue(creada.esSeguimiento()),
                () -> assertEquals(7, creada.getIntervalo()),
                () -> assertEquals(UnidadPeriodicidad.SEMANAS, creada.getUnidad()),
                () -> assertEquals(3, creada.getNumeroRepeticiones()),
                () -> assertEquals(4L, creada.getSolicitudOrigenId()));
    }

    @Test
    @DisplayName("Consultar un seguimiento devuelve su periodicidad")
    void consultarSeguimientoDevuelveLaPeriodicidad() {
        // Preparar
        estado = 200;
        respuesta = json(PERIODICIDAD);
        // Ejecutar
        SolicitudPrueba consultada = servicio.consultarSolicitud(MEDICO, 5L);
        // Comprobar
        assertAll(
                () -> assertEquals("/solicitudes/5", rutaRecibida),
                () -> assertTrue(consultada.esSeguimiento()),
                () -> assertEquals(UnidadPeriodicidad.SEMANAS, consultada.getUnidad()));
    }

    @Test
    @DisplayName("Una solicitud recibida sin periodicidad no es un seguimiento")
    void solicitudSinPeriodicidadNoEsSeguimiento() {
        // Preparar
        estado = 200;
        respuesta = json("");
        // Ejecutar
        SolicitudPrueba consultada = servicio.consultarSolicitud(MEDICO, 5L);
        // Comprobar
        assertFalse(consultada.esSeguimiento());
    }

    @Test
    @DisplayName("Una respuesta 400 por periodicidad inválida lanza IllegalArgumentException")
    void respuesta400LanzaDatosNoValidos() {
        // Preparar
        estado = 400;
        respuesta = "{\"error\":\"DATOS_INVALIDOS\"}";
        // Ejecutar y comprobar
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitud(MEDICO, datosSeguimiento()));
    }
}
