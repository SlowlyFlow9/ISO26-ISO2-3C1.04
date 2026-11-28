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
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

/** CU28 - Cliente: el cliente HTTP usa las rutas de la gestión de prioridades (D-039). */
class PrioridadesServiceHttpTest {

    private static final Usuario ADMINISTRATIVO = new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);

    private HttpServer servidor;
    private PrioridadesServiceHttp servicio;
    private volatile int estado;
    private volatile String respuesta;
    private volatile String metodoRecibido;
    private volatile String rutaRecibida;
    private volatile String consultaRecibida;
    private volatile String cuerpoRecibido;
    private volatile String rolRecibido;

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", exchange -> {
            metodoRecibido = exchange.getRequestMethod();
            rutaRecibida = exchange.getRequestURI().getPath();
            consultaRecibida = exchange.getRequestURI().getQuery();
            rolRecibido = exchange.getRequestHeaders().getFirst("X-Rol");
            cuerpoRecibido = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        servidor.start();
        servicio = new PrioridadesServiceHttp("http://127.0.0.1:" + servidor.getAddress().getPort() + "/");
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    private static String solicitud(long id, String prioridad) {
        return "{\"solicitudId\":" + id + ",\"pacienteId\":10,\"tipoPruebaId\":3,"
                + "\"prioridad\":\"" + prioridad + "\",\"fechaSolicitud\":\"2026-11-20T09:00:00\"}";
    }

    private static DatosCambioPrioridad cambio() {
        return new DatosCambioPrioridad(Prioridad.URGENTE, 2, "Empeoramiento");
    }

    // ---- Consultas ----

    @Test
    @DisplayName("Consultar las pendientes hace GET /prioridades con las cabeceras del usuario")
    void consultarPendientes() {
        // Preparar
        estado = 200;
        respuesta = "[" + solicitud(1, "URGENTE") + "," + solicitud(2, "ORDINARIA") + "]";
        // Ejecutar
        List<SolicitudPrioridad> lista = servicio.consultarPendientes(ADMINISTRATIVO, null);
        // Comprobar
        assertAll(
                () -> assertEquals("GET", metodoRecibido),
                () -> assertEquals("/prioridades", rutaRecibida),
                () -> assertNull(consultaRecibida),
                () -> assertEquals("PERSONAL_ADMINISTRATIVO", rolRecibido),
                () -> assertEquals(2, lista.size()),
                () -> assertEquals(Prioridad.ORDINARIA, lista.get(1).prioridad()),
                () -> assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), lista.get(0).fechaSolicitud()));
    }

    @Test
    @DisplayName("El filtro de prioridad se envía como parámetro")
    void consultarConFiltro() {
        // Preparar
        estado = 200;
        respuesta = "[]";
        // Ejecutar
        List<SolicitudPrioridad> lista = servicio.consultarPendientes(ADMINISTRATIVO, Prioridad.PREFERENTE);
        // Comprobar
        assertAll(
                () -> assertEquals("prioridad=PREFERENTE", consultaRecibida),
                () -> assertTrue(lista.isEmpty()));
    }

    @Test
    @DisplayName("Las reglas activas se piden a GET /prioridades/reglas")
    void consultarReglas() {
        // Preparar
        estado = 200;
        respuesta = "[{\"id\":2,\"nombre\":\"Oncológico\",\"descripcion\":\"D\",\"orden\":1}]";
        // Ejecutar
        List<ReglaPrioridadAplicable> reglas = servicio.consultarReglasActivas(ADMINISTRATIVO);
        // Comprobar
        assertAll(
                () -> assertEquals("/prioridades/reglas", rutaRecibida),
                () -> assertEquals(new ReglaPrioridadAplicable(2, "Oncológico", "D", 1), reglas.get(0)));
    }

    @Test
    @DisplayName("El historial se pide a GET /prioridades/{id}/historial")
    void consultarHistorial() {
        // Preparar
        estado = 200;
        respuesta = "[{\"id\":1,\"solicitudId\":5,\"prioridadAnterior\":\"ORDINARIA\",\"prioridadNueva\":\"URGENTE\","
                + "\"reglaPrioridadId\":2,\"motivo\":\"M\",\"realizadoPor\":\"admin\","
                + "\"fechaCambio\":\"2026-11-28T10:15:00\"}]";
        // Ejecutar
        List<CambioPrioridad> historial = servicio.consultarHistorial(ADMINISTRATIVO, 5);
        // Comprobar
        assertAll(
                () -> assertEquals("/prioridades/5/historial", rutaRecibida),
                () -> assertEquals(Prioridad.URGENTE, historial.get(0).prioridadNueva()),
                () -> assertEquals("admin", historial.get(0).realizadoPor()));
    }

    // ---- Cambio ----

    @Test
    @DisplayName("Cambiar la prioridad hace PUT /prioridades/{id} con la prioridad, la regla y el motivo")
    void cambiarPrioridad() {
        // Preparar
        estado = 200;
        respuesta = solicitud(5, "URGENTE");
        // Ejecutar
        SolicitudPrioridad actualizada = servicio.cambiarPrioridad(ADMINISTRATIVO, 5, cambio());
        // Comprobar
        assertAll(
                () -> assertEquals("PUT", metodoRecibido),
                () -> assertEquals("/prioridades/5", rutaRecibida),
                () -> assertEquals("{\"prioridad\":\"URGENTE\",\"reglaPrioridadId\":2,\"motivo\":\"Empeoramiento\"}",
                        cuerpoRecibido),
                () -> assertEquals(Prioridad.URGENTE, actualizada.prioridad()));
    }

    @Test
    @DisplayName("Sin datos no se llega a enviar la petición")
    void cambiarSinDatos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 5, null));
        assertNull(rutaRecibida);
    }

    // ---- Errores ----

    @Test
    @DisplayName("Los códigos de error se traducen a excepciones con mensaje para la pantalla")
    void codigosDeError() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> conCodigo(400)),
                () -> assertThrows(SecurityException.class, () -> conCodigo(403)),
                () -> assertThrows(NoSuchElementException.class, () -> conCodigo(404)),
                () -> assertTrue(assertThrows(IllegalStateException.class, () -> conCodigo(409))
                        .getMessage().contains("pendiente de citación")),
                () -> assertThrows(ServicioNoDisponibleException.class, () -> conCodigo(500)));
    }

    private void conCodigo(int codigo) {
        estado = codigo;
        respuesta = "{\"error\":\"X\"}";
        servicio.cambiarPrioridad(ADMINISTRATIVO, 5, cambio());
    }

    @ParameterizedTest
    @ValueSource(strings = {"no es json", "null", "[null]", "{\"a\":1}"})
    @DisplayName("Una respuesta con formato no válido lanza ServicioNoDisponibleException")
    void respuestaNoValida(String cuerpo) {
        // Preparar
        estado = 200;
        respuesta = cuerpo;
        // Ejecutar y comprobar
        assertThrows(ServicioNoDisponibleException.class, () -> servicio.consultarPendientes(ADMINISTRATIVO, null));
    }

    @Test
    @DisplayName("Una respuesta incompleta al cambiar la prioridad lanza ServicioNoDisponibleException")
    void respuestaCambioIncompleta() {
        // Preparar
        estado = 200;
        respuesta = "{\"solicitudId\":5}";
        // Ejecutar y comprobar
        assertThrows(ServicioNoDisponibleException.class, () -> servicio.cambiarPrioridad(ADMINISTRATIVO, 5, cambio()));
    }

    @Test
    @DisplayName("Sin servidor se lanza ServicioNoDisponibleException")
    void sinServidor() {
        // Preparar
        servidor.stop(0);
        // Ejecutar y comprobar
        assertThrows(ServicioNoDisponibleException.class, () -> servicio.consultarReglasActivas(ADMINISTRATIVO));
    }

    @Test
    @DisplayName("Una dirección vacía o un usuario no válido se rechazan")
    void datosNoValidos() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PrioridadesServiceHttp(" ")),
                () -> assertThrows(IllegalArgumentException.class, () -> new PrioridadesServiceHttp(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.consultarPendientes(null, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.consultarPendientes(new Usuario(" ", Rol.PERSONAL_ADMINISTRATIVO), null)));
    }
}
