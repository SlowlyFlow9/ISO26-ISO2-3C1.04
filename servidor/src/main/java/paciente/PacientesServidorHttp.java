package paciente;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposición HTTP del servicio (D-022): POST /pacientes con el usuario en las cabeceras X-Usuario y X-Rol (D-023).
 * Respuestas: 201 con el paciente; 400 {error, errores}; 403 / 409 {error, mensaje}; 405 y 404 {error}.
 * ponytail: solo escucha en localhost porque no hay autenticación real; ampliar cuando exista el SSO.
 */
public class PacientesServidorHttp {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (f, t, c) -> new JsonPrimitive(f.toString()))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (j, t, c) -> LocalDate.parse(j.getAsString()))
            .create();

    private PacientesServidorHttp() { }

    /** Arranca el servidor en el puerto indicado (0 = uno libre) y lo devuelve ya iniciado. */
    public static HttpServer iniciar(PacientesService servicio, int puerto) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), puerto), 0);
        servidor.createContext("/", exchange -> {
            try {
                atender(servicio, exchange);
            } catch (RuntimeException e) {
                responder(exchange, 500, error("ERROR_INTERNO", null));
            }
        });
        servidor.start();
        return servidor;
    }

    private static void atender(PacientesService servicio, HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/pacientes")) {
            responder(exchange, 404, error("NO_ENCONTRADO", null));
            return;
        }
        if (!exchange.getRequestMethod().equals("POST")) {
            responder(exchange, 405, error("METODO_NO_PERMITIDO", null));
            return;
        }
        Usuario usuario = usuario(exchange);
        String cuerpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Paciente datos;
        try {
            datos = GSON.fromJson(cuerpo, Paciente.class);
        } catch (JsonSyntaxException e) {
            datos = null;
        }
        try {
            if (usuario == null) throw new SinPermisoException("Falta el usuario o su rol no es válido");
            if (datos == null) throw new DatosInvalidosException(List.of("El cuerpo de la petición no es válido"));
            responder(exchange, 201, GSON.toJson(servicio.crear(usuario, datos)));
        } catch (SinPermisoException e) {
            responder(exchange, 403, error("SIN_PERMISO", e.getMessage()));
        } catch (DatosInvalidosException e) {
            JsonObject json = new JsonObject();
            json.addProperty("error", "DATOS_INVALIDOS");
            json.add("errores", GSON.toJsonTree(e.getErrores()));
            responder(exchange, 400, json.toString());
        } catch (PacienteDuplicadoException e) {
            responder(exchange, 409, error("DUPLICADO", e.getMessage()));
        }
    }

    private static Usuario usuario(HttpExchange exchange) {
        String rol = exchange.getRequestHeaders().getFirst("X-Rol");
        if (rol == null) return null;
        try {
            String nombre = exchange.getRequestHeaders().getFirst("X-Usuario");
            return new Usuario(nombre == null ? "desconocido" : nombre, Rol.valueOf(rol));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String error(String codigo, String mensaje) {
        JsonObject json = new JsonObject();
        json.addProperty("error", codigo);
        if (mensaje != null) json.addProperty("mensaje", mensaje);
        return json.toString();
    }

    private static void responder(HttpExchange exchange, int estado, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(estado, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
