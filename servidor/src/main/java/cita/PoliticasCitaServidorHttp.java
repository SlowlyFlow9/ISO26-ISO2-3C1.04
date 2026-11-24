package cita;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public final class PoliticasCitaServidorHttp {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    new AdaptadorFechaHora())
            .create();

    private PoliticasCitaServidorHttp() {
    }

    public static void registrar(
            HttpServer servidor,
            PoliticaCitaService servicio) {
        if (servidor == null || servicio == null) {
            throw new IllegalArgumentException(
                    "El servidor y servicio son obligatorios");
        }

        servidor.createContext("/politicas-citacion", exchange -> {
            try {
                atender(exchange, servicio);
            } catch (SinPermisoException e) {
                responder(exchange, 403,
                        error("SIN_PERMISO", e.getMessage()));
            } catch (DatosPoliticaCitaInvalidosException
                    | IllegalArgumentException
                    | JsonParseException e) {
                responder(exchange, 400,
                        error("DATOS_INVALIDOS", e.getMessage()));
            } catch (IOException e) {
                responder(exchange, 400,
                        error("DATOS_INVALIDOS",
                                "No se pudo leer la solicitud"));
            }
        });
    }

    private static void atender(
            HttpExchange exchange,
            PoliticaCitaService servicio)
            throws IOException, SinPermisoException,
            DatosPoliticaCitaInvalidosException {
        if (!exchange.getRequestURI().getPath()
                .equals("/politicas-citacion")) {
            responder(exchange, 404,
                    error("NO_ENCONTRADO", null));
            return;
        }

        Usuario solicitante = usuario(exchange);
        String metodo = exchange.getRequestMethod();
        if (metodo.equals("GET")) {
            responder(exchange, 200,
                    GSON.toJson(servicio.consultar(solicitante)));
            return;
        }
        if (metodo.equals("PUT")) {
            String cuerpo = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);
            PoliticaCita datos = GSON.fromJson(
                    cuerpo, PoliticaCita.class);
            responder(exchange, 200,
                    GSON.toJson(servicio.modificar(
                            solicitante, datos)));
            return;
        }
        responder(exchange, 405,
                error("METODO_NO_PERMITIDO", null));
    }

    private static Usuario usuario(HttpExchange exchange) {
        String nombre = exchange.getRequestHeaders()
                .getFirst("X-Usuario");
        String rol = exchange.getRequestHeaders()
                .getFirst("X-Rol");
        if (nombre == null || nombre.isBlank() || rol == null) {
            return null;
        }
        try {
            return new Usuario(nombre, Rol.valueOf(rol));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String error(String codigo, String mensaje) {
        JsonObject json = new JsonObject();
        json.addProperty("error", codigo);
        if (mensaje != null) {
            json.addProperty("mensaje", mensaje);
        }
        return json.toString();
    }

    private static void responder(
            HttpExchange exchange,
            int estado,
            String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(estado, bytes.length);
        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
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
            try {
                return LocalDateTime.parse(json.getAsString());
            } catch (RuntimeException e) {
                throw new JsonParseException(
                        "Fecha y hora no validas", e);
            }
        }
    }
}
