package agenda;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

/**
 * CU29 - Pruebas del cliente HTTP de huecos.
 *
 * Utiliza un servidor temporal para comprobar
 * peticiones, respuestas y gestión de errores.
 */
public class HuecosAgendaServiceHttpTest {

    private HttpServer servidor;
    private HuecosAgendaServiceHttp servicio;

    private final Usuario administrativo = new Usuario(
            "Aitana", Rol.PERSONAL_ADMINISTRATIVO);

    // Datos configurables de la respuesta simulada.
    private volatile int codigoRespuesta;
    private volatile String jsonRespuesta;

    // Datos recibidos por el servidor de pruebas.
    private volatile String metodoRecibido;
    private volatile String rutaRecibida;
    private volatile String cuerpoRecibido;
    private volatile String usuarioRecibido;
    private volatile String rolRecibido;

    @BeforeEach
    void preparar() throws IOException {

        codigoRespuesta = 200;
        jsonRespuesta = "[]";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/huecos", exchange -> {

            metodoRecibido = exchange.getRequestMethod();
            rutaRecibida = exchange.getRequestURI().toString();

            usuarioRecibido = exchange.getRequestHeaders()
                    .getFirst("X-Usuario");

            rolRecibido = exchange.getRequestHeaders()
                    .getFirst("X-Rol");

            cuerpoRecibido = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            byte[] bytes = jsonRespuesta.getBytes(
                    StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json");

            exchange.sendResponseHeaders(
                    codigoRespuesta, bytes.length);

            try (var salida = exchange.getResponseBody()) {
                salida.write(bytes);
            }
        });

        servidor.start();

        servicio = new HuecosAgendaServiceHttp(
                "http://127.0.0.1:"
                + servidor.getAddress().getPort());
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private HuecoAgenda crearDatos() {

        HuecoAgenda datos = new HuecoAgenda();
        datos.setAgendaId(1L);
        datos.setHoraInicio(LocalTime.of(9, 0));
        datos.setHoraFin(LocalTime.of(9, 30));

        return datos;
    }

    private void devolverHueco(boolean bloqueado) {

        codigoRespuesta = 200;

        jsonRespuesta =
                "{\"id\":7,\"agendaId\":1,"
                + "\"horaInicio\":\"09:00\","
                + "\"horaFin\":\"09:30\","
                + "\"bloqueado\":" + bloqueado + "}";
    }

    @Test
    void crearEnviaPostYRecibeHueco() {

        devolverHueco(false);
        codigoRespuesta = 201;

        HuecoAgenda creado = servicio.crear(
                administrativo, crearDatos());

        assertEquals("POST", metodoRecibido);
        assertEquals("/huecos", rutaRecibida);

        assertEquals("Aitana", usuarioRecibido);
        assertEquals("PERSONAL_ADMINISTRATIVO",
                rolRecibido);

        var json = JsonParser.parseString(cuerpoRecibido)
                .getAsJsonObject();

        assertEquals(1L, json.get("agendaId").getAsLong());
        assertEquals("09:00",
                json.get("horaInicio").getAsString());

        assertEquals(7L, creado.getId());
        assertFalse(creado.isBloqueado());
    }

    @Test
    void listarHuecosPorAgenda() {

        jsonRespuesta =
                "[{\"id\":1,\"agendaId\":4,"
                + "\"horaInicio\":\"09:00\","
                + "\"horaFin\":\"09:30\","
                + "\"bloqueado\":false},"
                + "{\"id\":2,\"agendaId\":4,"
                + "\"horaInicio\":\"10:00\","
                + "\"horaFin\":\"10:30\","
                + "\"bloqueado\":true}]";

        List<HuecoAgenda> resultado =
                servicio.listarPorAgenda(administrativo, 4L);

        assertEquals("GET", metodoRecibido);
        assertEquals("/huecos?agendaId=4", rutaRecibida);

        assertEquals(2, resultado.size());
        assertEquals(LocalTime.of(9, 0),
                resultado.get(0).getHoraInicio());

        assertFalse(resultado.get(0).isBloqueado());
        assertTrue(resultado.get(1).isBloqueado());
    }

    @Test
    void listarAgendaSinHuecos() {

        // El servidor responde con una lista vacía.
        jsonRespuesta = "[]";

        List<HuecoAgenda> resultado =
                servicio.listarPorAgenda(administrativo, 1L);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void bloquearHuecoPorHttp() {

        devolverHueco(true);

        HuecoAgenda resultado =
                servicio.bloquear(administrativo, 7L);

        assertEquals("POST", metodoRecibido);
        assertEquals("/huecos/7/bloquear", rutaRecibida);
        assertTrue(resultado.isBloqueado());
    }

    @Test
    void liberarHuecoPorHttp() {

        devolverHueco(false);

        HuecoAgenda resultado =
                servicio.liberar(administrativo, 7L);

        assertEquals("POST", metodoRecibido);
        assertEquals("/huecos/7/liberar", rutaRecibida);
        assertFalse(resultado.isBloqueado());
    }

    @Test
    void gestionarErroresHttp() {

        codigoRespuesta = 400;
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(
                        administrativo, crearDatos()));

        codigoRespuesta = 403;
        assertThrows(SecurityException.class,
                () -> servicio.listarPorAgenda(
                        administrativo, 1L));

        codigoRespuesta = 404;
        assertThrows(NoSuchElementException.class,
                () -> servicio.bloquear(
                        administrativo, 999L));

        codigoRespuesta = 409;
        assertThrows(IllegalStateException.class,
                () -> servicio.liberar(
                        administrativo, 7L));

        codigoRespuesta = 500;
        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listarPorAgenda(
                        administrativo, 1L));
    }

    @Test
    void rechazarDatosNulos() {

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(
                        administrativo, null));
    }

    @Test
    void rechazarIdentificadoresInvalidos() {

        assertThrows(IllegalArgumentException.class,
                () -> servicio.listarPorAgenda(
                        administrativo, 0L));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.bloquear(
                        administrativo, -1L));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.liberar(
                        administrativo, 0L));
    }

    @Test
    void rechazarUsuarioInvalido() {

        assertThrows(IllegalArgumentException.class,
                () -> servicio.listarPorAgenda(null, 1L));

        Usuario sinNombre = new Usuario(
                " ", Rol.PERSONAL_ADMINISTRATIVO);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.listarPorAgenda(
                        sinNombre, 1L));

        Usuario sinRol = new Usuario("Aitana", null);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.listarPorAgenda(
                        sinRol, 1L));
    }

    @Test
    void rechazarRespuestaJsonInvalida() {

        codigoRespuesta = 201;
        jsonRespuesta = "esto no es JSON";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.crear(
                        administrativo, crearDatos()));
    }

    @Test
    void rechazarRespuestaIncompleta() {

        codigoRespuesta = 201;
        jsonRespuesta = "{}";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.crear(
                        administrativo, crearDatos()));
    }

    @Test
    void rechazarListadoNulo() {

        codigoRespuesta = 200;
        jsonRespuesta = "null";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listarPorAgenda(
                        administrativo, 1L));
    }

    @Test
    void rechazarDireccionServidorInvalida() {

        assertThrows(IllegalArgumentException.class,
                () -> new HuecosAgendaServiceHttp(null));

        assertThrows(IllegalArgumentException.class,
                () -> new HuecosAgendaServiceHttp("  "));
    }

}
