package agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.time.LocalTime;
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

class HorarioServicioServiceHttpTest {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalTime.class, new AdaptadorHora())
            .registerTypeAdapter(
                    LocalDateTime.class, new AdaptadorFechaHora())
            .create();

    private HttpServer servidor;
    private HorarioServicioServiceHttp servicio;
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
                "/horarios-servicio", this::responder);
        servidor.start();
        servicio = new HorarioServicioServiceHttp(
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
                ? GSON.toJson(horarioBase())
                : "{\"error\":\"DATOS_INVALIDOS\","
                        + "\"mensaje\":\"Horario no valido\"}";
        byte[] bytes = cuerpo.getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json");
        exchange.sendResponseHeaders(estado, bytes.length);
        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private HorarioServicio horarioBase() {
        HorarioServicio horario = HorarioServicio.vacio();
        horario.getDias().get(0).getIntervalos().add(
                new IntervaloHorario(
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0)));
        horario.setModificadoPor("admin");
        horario.setModificadoEn(
                LocalDateTime.of(2026, 11, 23, 10, 0));
        return horario;
    }

    private Usuario admin() {
        return new Usuario("admin", Rol.ADMINISTRADOR);
    }

    @Test
    void consultarHorarioDeserializaHorasYAuditoria()
            throws Exception {
        HorarioServicio horario = servicio.consultar(admin());

        assertEquals("GET", metodoRecibido.get());
        assertEquals(LocalTime.of(8, 0),
                horario.getDias().get(0).getIntervalos()
                        .get(0).getHoraInicio());
        assertEquals("admin", horario.getModificadoPor());
        assertEquals(LocalDateTime.of(2026, 11, 23, 10, 0),
                horario.getModificadoEn());
    }

    @Test
    void modificarHorarioEnvíaPutConIdentidad()
            throws Exception {
        servicio.modificar(admin(), horarioBase());

        assertEquals("PUT", metodoRecibido.get());
        assertTrue(cuerpoRecibido.get()
                .contains("\"horaInicio\":\"08:00\""));
    }

    @Test
    void rechazarUsuarioNoAdministradorAntesDeEnviar() {
        assertThrows(SinPermisoException.class,
                () -> servicio.consultar(
                        new Usuario("medico", Rol.MEDICO)));
        assertEquals(null, metodoRecibido.get());
    }

    @Test
    void propagarErroresDeValidacionDelServidor() {
        estadoRespuesta.set(400);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultar(admin()));

        assertEquals("Horario no valido", error.getMessage());
    }

    private static final class AdaptadorHora
            implements JsonSerializer<LocalTime>,
                    JsonDeserializer<LocalTime> {
        @Override
        public JsonElement serialize(
                LocalTime value,
                Type type,
                JsonSerializationContext context) {
            return new JsonPrimitive(value.toString());
        }

        @Override
        public LocalTime deserialize(
                JsonElement json,
                Type type,
                JsonDeserializationContext context)
                throws JsonParseException {
            return LocalTime.parse(json.getAsString());
        }
    }

    private static final class AdaptadorFechaHora
            implements JsonSerializer<LocalDateTime>,
                    JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(
                LocalDateTime value,
                Type type,
                JsonSerializationContext context) {
            return new JsonPrimitive(value.toString());
        }

        @Override
        public LocalDateTime deserialize(
                JsonElement json,
                Type type,
                JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }
}
