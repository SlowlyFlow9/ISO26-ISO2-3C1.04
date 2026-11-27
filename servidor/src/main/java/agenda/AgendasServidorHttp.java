package agenda;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

/** CU14 - Comunicación HTTP para gestionar agendas de realización. */
public final class AgendasServidorHttp {

    private static class AdaptadorFecha
            implements JsonSerializer<LocalDate>,
                       JsonDeserializer<LocalDate> {

        @Override
        public JsonElement serialize(
                LocalDate fecha,
                Type tipo,
                JsonSerializationContext contexto) {

            return new JsonPrimitive(fecha.toString());
        }

        @Override
        public LocalDate deserialize(
                JsonElement json,
                Type tipo,
                JsonDeserializationContext contexto)
                throws JsonParseException {

            try {
                return LocalDate.parse(json.getAsString());
            } catch (RuntimeException e) {
                throw new JsonParseException(
                        "Fecha no válida", e);
            }
        }
    }

    private static class AdaptadorHora
            implements JsonSerializer<LocalTime>,
                       JsonDeserializer<LocalTime> {

        @Override
        public JsonElement serialize(
                LocalTime hora,
                Type tipo,
                JsonSerializationContext contexto) {

            return new JsonPrimitive(hora.toString());
        }

        @Override
        public LocalTime deserialize(
                JsonElement json,
                Type tipo,
                JsonDeserializationContext contexto)
                throws JsonParseException {

            try {
                return LocalTime.parse(json.getAsString());
            } catch (RuntimeException e) {
                throw new JsonParseException(
                        "Hora no válida", e);
            }
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDate.class,
                    new AdaptadorFecha())
            .registerTypeAdapter(
                    LocalTime.class,
                    new AdaptadorHora())
            .create();

    private AgendasServidorHttp() {
    }

    public static void registrar(
            HttpServer servidor,
            AgendasRealizacionService servicio) {

        if (servidor == null || servicio == null) {
            throw new IllegalArgumentException(
                    "El servidor y el servicio son obligatorios");
        }

        servidor.createContext(
                "/agendas-realizacion",
                exchange -> {
                    try {
                        atender(exchange, servicio);
                    } catch (SinPermisoException e) {
                        responder(exchange, 403,
                                error("SIN_PERMISO", e.getMessage()));
                    } catch (AgendaNoEncontradaException e) {
                        responder(exchange, 404,
                                error("NO_ENCONTRADO", e.getMessage()));
                    } catch (DatosAgendaInvalidosException e) {
                        responder(exchange, 400,
                                error("DATOS_INVALIDOS", e.getMessage()));
                    } catch (IllegalArgumentException
                            | JsonParseException e) {
                        responder(exchange, 400,
                                error("DATOS_INVALIDOS", e.getMessage()));
                    } catch (RuntimeException e) {
                        responder(exchange, 500,
                                error("ERROR_INTERNO",
                                        "Error interno del servidor"));
                    }
                });
    }

    private static void atender(
            HttpExchange exchange,
            AgendasRealizacionService servicio)
            throws IOException,
            SinPermisoException,
            AgendaNoEncontradaException,
            DatosAgendaInvalidosException {

        String ruta = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();
        Usuario solicitante = usuario(exchange);

        if (ruta.equals("/agendas-realizacion")) {
            if (metodo.equals("POST")) {
                AgendaMedica datos = GSON.fromJson(
                        new String(
                                exchange.getRequestBody().readAllBytes(),
                                StandardCharsets.UTF_8),
                        AgendaMedica.class);

                AgendaMedica creada =
                        servicio.crearPropia(solicitante, datos);

                responder(exchange, 201, GSON.toJson(creada));
                return;
            }

            if (metodo.equals("GET")) {
                List<AgendaMedica> resultado =
                        servicio.listarPropias(
                                solicitante);

                responder(exchange, 200, GSON.toJson(resultado));
                return;
            }

            responder(exchange, 405,
                    error("METODO_NO_PERMITIDO", null));
            return;
        }

        String prefijo = "/agendas-realizacion/";
        if (ruta.startsWith(prefijo)) {
            long id = leerId(ruta.substring(prefijo.length()));
            if (id <= 0) {
                throw new IllegalArgumentException(
                        "El identificador debe ser positivo");
            }

            if (metodo.equals("GET")) {
                AgendaMedica agenda =
                        servicio.consultarPropia(solicitante, id);

                responder(exchange, 200, GSON.toJson(agenda));
                return;
            }

            if (metodo.equals("PUT")) {
                AgendaMedica datos = GSON.fromJson(
                        new String(
                                exchange.getRequestBody().readAllBytes(),
                                StandardCharsets.UTF_8),
                        AgendaMedica.class);

                AgendaMedica modificada =
                        servicio.modificarPropia(
                                solicitante, id, datos);

                responder(exchange, 200, GSON.toJson(modificada));
                return;
            }

            if (metodo.equals("DELETE")) {
                servicio.eliminarPropia(solicitante, id);
                responderVacio(exchange, 204);
                return;
            }

            responder(exchange, 405,
                    error("METODO_NO_PERMITIDO", null));
            return;
        }

        responder(exchange, 404,
                error("NO_ENCONTRADO", null));
    }

    private static long leerId(String valor) {
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El identificador debe ser numérico", e);
        }
    }

    private static Usuario usuario(HttpExchange exchange) {
        String nombre = exchange.getRequestHeaders()
                .getFirst("X-Usuario");
        String rol = exchange.getRequestHeaders()
                .getFirst("X-Rol");
        String medicoId = exchange.getRequestHeaders()
                .getFirst("X-Medico-Id");

        if (nombre == null || nombre.isBlank()
                || rol == null
                || medicoId == null || medicoId.isBlank()) {
            return null;
        }

        try {
            long id = Long.parseLong(medicoId);
            if (id <= 0) {
                return null;
            }

            return new Usuario(
                    nombre,
                    Rol.valueOf(rol),
                    id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String error(
            String codigo,
            String mensaje) {

        JsonObject json = new JsonObject();
        json.addProperty("error", codigo);
        if (mensaje != null) {
            json.addProperty("mensaje", mensaje);
        }
        return GSON.toJson(json);
    }

    private static void responder(
            HttpExchange exchange,
            int estado,
            String cuerpo) throws IOException {

        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8");
        exchange.sendResponseHeaders(estado, bytes.length);

        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private static void responderVacio(
            HttpExchange exchange,
            int estado) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8");
        exchange.sendResponseHeaders(estado, -1);
        exchange.close();
    }
}
