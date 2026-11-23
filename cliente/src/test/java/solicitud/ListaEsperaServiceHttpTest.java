package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

class ListaEsperaServiceHttpTest {

    private static final Usuario ADMINISTRATIVO =
            new Usuario("administrativo", Rol.PERSONAL_ADMINISTRATIVO);

    private HttpServer servidor;
    private ListaEsperaServiceHttp servicio;
    private volatile int codigoRespuesta;
    private volatile String jsonRespuesta;
    private volatile String rutaRecibida;
    private volatile String usuarioRecibido;
    private volatile String rolRecibido;

    @BeforeEach
    void setUp() throws IOException {
        codigoRespuesta = 200;
        jsonRespuesta = "[]";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/listas-espera", exchange -> {
            rutaRecibida = exchange.getRequestURI().toString();
            usuarioRecibido = exchange.getRequestHeaders()
                    .getFirst("X-Usuario");
            rolRecibido = exchange.getRequestHeaders()
                    .getFirst("X-Rol");

            byte[] cuerpo = jsonRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json");
            exchange.sendResponseHeaders(codigoRespuesta, cuerpo.length);
            try (var salida = exchange.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();

        servicio = new ListaEsperaServiceHttp(
                "http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("Consulta la lista y convierte sus fechas")
    void consultaLista() {
        jsonRespuesta = "[{\"solicitudId\":7,\"pacienteId\":12,"
                + "\"tipoPruebaId\":4,\"prioridad\":\"URGENTE\","
                + "\"fechaSolicitud\":\"2026-11-20T10:30:00\"}]";

        List<EntradaListaEspera> entradas =
                servicio.consultar(ADMINISTRATIVO, null);

        assertAll(
                () -> assertEquals("/listas-espera", rutaRecibida),
                () -> assertEquals("administrativo", usuarioRecibido),
                () -> assertEquals("PERSONAL_ADMINISTRATIVO", rolRecibido),
                () -> assertEquals(1, entradas.size()),
                () -> assertEquals(7, entradas.get(0).solicitudId()),
                () -> assertEquals(12, entradas.get(0).pacienteId()),
                () -> assertEquals(4, entradas.get(0).tipoPruebaId()),
                () -> assertEquals(Prioridad.URGENTE, entradas.get(0).prioridad()),
                () -> assertEquals(
                        LocalDateTime.of(2026, 11, 20, 10, 30),
                        entradas.get(0).fechaSolicitud()));
    }

    @Test
    @DisplayName("Envía el filtro de prioridad")
    void enviaFiltroPrioridad() {
        servicio.consultar(ADMINISTRATIVO, Prioridad.PREFERENTE);

        assertEquals(
                "/listas-espera?prioridad=PREFERENTE",
                rutaRecibida);
    }

    @Test
    @DisplayName("Traduce los errores HTTP y rechaza respuestas inválidas")
    void gestionaErrores() {
        codigoRespuesta = 400;
        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultar(ADMINISTRATIVO, null));

        codigoRespuesta = 403;
        assertThrows(SecurityException.class,
                () -> servicio.consultar(ADMINISTRATIVO, null));

        codigoRespuesta = 404;
        assertThrows(NoSuchElementException.class,
                () -> servicio.consultar(ADMINISTRATIVO, null));

        codigoRespuesta = 500;
        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.consultar(ADMINISTRATIVO, null));

        codigoRespuesta = 200;
        jsonRespuesta = "null";
        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.consultar(ADMINISTRATIVO, null));
    }

    @Test
    @DisplayName("Rechaza usuario y URL inválidos")
    void validaDatosDeConexion() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> servicio.consultar(null, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ListaEsperaServiceHttp(" ")),
                () -> assertTrue(assertThrows(
                        IllegalArgumentException.class,
                        () -> servicio.consultar(
                                new Usuario(" ", Rol.PERSONAL_ADMINISTRATIVO),
                                null)).getMessage().contains("usuario")));
    }
}
