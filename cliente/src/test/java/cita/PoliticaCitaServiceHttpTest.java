package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class PoliticaCitaServiceHttpTest {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    new AdaptadorFechaHora())
            .create();

    private HttpServer servidor;
    private PoliticaCitaServiceHttp servicio;
    private final AtomicInteger estadoRespuesta =
            new AtomicInteger(200);
    private final AtomicReference<String> metodoRecibido =
            new AtomicReference<>();
    private final AtomicReference<String> cuerpoRecibido =
            new AtomicReference<>();

    @BeforeEach
    void preparar() throws IOException {
        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(
                "/politicas-citacion", this::responder);
        servidor.start();
        servicio = new PoliticaCitaServiceHttp(
                "http://127.0.0.1:"
                        + servidor.getAddress().getPort());
    }

    @AfterEach
    void cerrar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private void responder(HttpExchange exchange)
            throws IOException {
        metodoRecibido.set(exchange.getRequestMethod());
        cuerpoRecibido.set(new String(
                exchange.getRequestBody().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8));
        int estado = estadoRespuesta.get();
        String cuerpo = estado == 200
                ? GSON.toJson(politicaValida())
                : "{\"error\":\"DATOS_INVALIDOS\","
                        + "\"mensaje\":\"Politica no valida\"}";
        byte[] bytes = cuerpo.getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json");
        exchange.sendResponseHeaders(estado, bytes.length);
        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private PoliticaCita politicaValida() {
        PoliticaCita politica = PoliticaCita.inicial();
        politica.setModificadoPor("admin");
        politica.setModificadoEn(
                LocalDateTime.of(2026, 11, 23, 10, 0));
        return politica;
    }

    private Usuario admin() {
        return new Usuario("admin", Rol.ADMINISTRADOR);
    }

    @Test
    void consultarDeserializaPoliticaYAuditoria()
            throws Exception {
        PoliticaCita politica = servicio.consultar(admin());

        assertEquals("GET", metodoRecibido.get());
        assertEquals(90, politica.getAntelacionMaximaDias());
        assertEquals("admin", politica.getModificadoPor());
        assertEquals(LocalDateTime.of(2026, 11, 23, 10, 0),
                politica.getModificadoEn());
    }

    @Test
    void modificarEnviaPutConLosCuatroValores()
            throws Exception {
        servicio.modificar(admin(), politicaValida());

        assertEquals("PUT", metodoRecibido.get());
        assertTrue(cuerpoRecibido.get()
                .contains("\"cancelacionMinimaHoras\":24"));
        assertTrue(cuerpoRecibido.get()
                .contains("\"maxCitasActivasPorPaciente\":3"));
    }

    @Test
    void rechazarUsuarioNoAdministradorAntesDeEnviar() {
        assertThrows(SinPermisoException.class,
                () -> servicio.consultar(
                        new Usuario("medico", Rol.MEDICO)));
        assertNull(metodoRecibido.get());
    }

    @Test
    void propagarErroresDeValidacionDelServidor() {
        estadoRespuesta.set(400);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultar(admin()));

        assertEquals("Politica no valida", error.getMessage());
    }

    private static final class AdaptadorFechaHora
            implements JsonSerializer<LocalDateTime>,
                    JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(
                LocalDateTime fecha,
                Type tipo,
                JsonSerializationContext contexto) {
            return new JsonPrimitive(fecha.toString());
        }

        @Override
        public LocalDateTime deserialize(
                JsonElement json,
                Type tipo,
                JsonDeserializationContext contexto)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }
}
