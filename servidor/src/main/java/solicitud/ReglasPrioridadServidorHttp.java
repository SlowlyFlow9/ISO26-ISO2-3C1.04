package solicitud;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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

public final class ReglasPrioridadServidorHttp {

    private static final String RUTA = "/reglas-prioridad";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    new AdaptadorFechaHora())
            .create();

    private ReglasPrioridadServidorHttp() {
    }

    public static void registrar(
            HttpServer servidor,
            ReglasPrioridadService servicio) {
        if (servidor == null || servicio == null) {
            throw new IllegalArgumentException(
                    "El servidor y servicio son obligatorios");
        }
        servidor.createContext(RUTA, exchange -> {
            try {
                atender(exchange, servicio);
            } catch (SinPermisoException e) {
                responder(exchange, 403,
                        error("SIN_PERMISO", e.getMessage()));
            } catch (DatosReglaPrioridadInvalidosException
                    | JsonParseException
                    | IllegalArgumentException e) {
                responder(exchange, 400,
                        error("DATOS_INVALIDOS", e.getMessage()));
            } catch (ReglaPrioridadDuplicadaException e) {
                responder(exchange, 409,
                        error("REGLA_DUPLICADA", e.getMessage()));
            } catch (ReglaPrioridadNoEncontradaException e) {
                responder(exchange, 404,
                        error("NO_ENCONTRADO", e.getMessage()));
            } catch (IOException e) {
                responder(exchange, 400,
                        error("DATOS_INVALIDOS",
                                "No se pudo leer la solicitud"));
            }
        });
    }

    private static void atender(
            HttpExchange exchange,
            ReglasPrioridadService servicio)
            throws IOException,
            SinPermisoException,
            DatosReglaPrioridadInvalidosException,
            ReglaPrioridadDuplicadaException,
            ReglaPrioridadNoEncontradaException {
        String path = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();
        Usuario solicitante = usuario(exchange);

        if (path.equals(RUTA)) {
            if (metodo.equals("GET")) {
                List<ReglaPrioridad> reglas =
                        servicio.listar(solicitante);
                responder(exchange, 200, GSON.toJson(reglas));
                return;
            }
            if (metodo.equals("POST")) {
                ReglaPrioridad creada = servicio.crear(
                        solicitante, leerDatos(exchange));
                exchange.getResponseHeaders().set(
                        "Location", RUTA + "/" + creada.getId());
                responder(exchange, 201, GSON.toJson(creada));
                return;
            }
            responder(exchange, 405,
                    error("METODO_NO_PERMITIDO", null));
            return;
        }

        if (!path.startsWith(RUTA + "/")) {
            responder(exchange, 404, error("NO_ENCONTRADO", null));
            return;
        }

        long id = obtenerId(path.substring((RUTA + "/").length()));
        if (metodo.equals("GET")) {
            responder(exchange, 200, GSON.toJson(
                    servicio.consultar(solicitante, id)));
        } else if (metodo.equals("PUT")) {
            ReglaPrioridad modificada = servicio.modificar(
                    solicitante, id, leerDatos(exchange));
            responder(exchange, 200, GSON.toJson(modificada));
        } else if (metodo.equals("DELETE")) {
            servicio.desactivar(solicitante, id);
            responderVacio(exchange, 204);
        } else {
            responder(exchange, 405,
                    error("METODO_NO_PERMITIDO", null));
        }
    }

    private static DatosReglaPrioridad leerDatos(
            HttpExchange exchange)
            throws IOException {
        String cuerpo = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
        return GSON.fromJson(cuerpo, DatosReglaPrioridad.class);
    }

    private static long obtenerId(String texto) {
        try {
            long id = Long.parseLong(texto);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El identificador de la regla no es válido", e);
        }
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

    private static void responderVacio(
            HttpExchange exchange,
            int estado) throws IOException {
        exchange.sendResponseHeaders(estado, -1);
        exchange.close();
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
                        "Fecha y hora no válidas", e);
            }
        }
    }
}
