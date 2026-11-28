package solicitud;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

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

/**
 * CU28 - Rutas HTTP de la gestion de prioridades (D-039), con JSON (D-022):
 * GET /prioridades?prioridad=, GET /prioridades/reglas, PUT /prioridades/{id}
 * y GET /prioridades/{id}/historial.
 */
public final class PrioridadesServidorHttp {

    private static final String RUTA = "/prioridades";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new AdaptadorFecha())
            .create();

    private PrioridadesServidorHttp() {
    }

    //Registra las rutas de CU28 en un servidor HTTP existente
    public static void registrar(HttpServer servidor, PrioridadesService servicio) {

        if (servidor == null || servicio == null) {
            throw new IllegalArgumentException("El servidor y el servicio son obligatorios");
        }

        servidor.createContext(RUTA, exchange -> {
            try {
                atender(exchange, servicio);

            } catch (SinPermisoException e) {
                responder(exchange, 403, error("SIN_PERMISO"));

            } catch (NoSuchElementException e) {
                responder(exchange, 404, error("NO_ENCONTRADO"));

            } catch (SolicitudNoGestionableException e) {
                responder(exchange, 409, error("ESTADO_NO_PERMITIDO"));

            } catch (IllegalArgumentException | JsonParseException e) {
                responder(exchange, 400, error("DATOS_INVALIDOS"));

            } catch (RuntimeException e) {
                responder(exchange, 500, error("ERROR_INTERNO"));
            }
        });
    }

    private static void atender(HttpExchange exchange, PrioridadesService servicio)
            throws IOException, SinPermisoException {

        String ruta = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();

        //GET /prioridades?prioridad=
        if (ruta.equals(RUTA)) {
            if (!metodo.equals("GET")) {
                responder(exchange, 405, error("METODO_NO_PERMITIDO"));
                return;
            }
            Prioridad prioridad = leerPrioridad(exchange.getRequestURI().getRawQuery());
            responder(exchange, 200, GSON.toJson(
                    servicio.consultarPendientes(usuario(exchange), prioridad)));
            return;
        }

        //GET /prioridades/reglas
        if (ruta.equals(RUTA + "/reglas")) {
            if (!metodo.equals("GET")) {
                responder(exchange, 405, error("METODO_NO_PERMITIDO"));
                return;
            }
            responder(exchange, 200, GSON.toJson(servicio.consultarReglasActivas(usuario(exchange))));
            return;
        }

        //GET /prioridades/{id}/historial
        if (ruta.startsWith(RUTA + "/") && ruta.endsWith("/historial")) {
            if (!metodo.equals("GET")) {
                responder(exchange, 405, error("METODO_NO_PERMITIDO"));
                return;
            }
            long id = leerId(ruta.substring((RUTA + "/").length(), ruta.length() - "/historial".length()));
            responder(exchange, 200, GSON.toJson(servicio.consultarHistorial(usuario(exchange), id)));
            return;
        }

        //PUT /prioridades/{id}
        if (ruta.startsWith(RUTA + "/")) {
            if (!metodo.equals("PUT")) {
                responder(exchange, 405, error("METODO_NO_PERMITIDO"));
                return;
            }
            long id = leerId(ruta.substring((RUTA + "/").length()));
            String cuerpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            DatosCambioPrioridad datos = GSON.fromJson(cuerpo, DatosCambioPrioridad.class);
            responder(exchange, 200, GSON.toJson(servicio.cambiarPrioridad(usuario(exchange), id, datos)));
            return;
        }

        responder(exchange, 404, error("NO_ENCONTRADO"));
    }

    //Lee el filtro opcional ?prioridad=; un valor no valido produce un 400
    static Prioridad leerPrioridad(String consulta) {
        if (consulta == null || consulta.isBlank()) {
            return null;
        }
        Prioridad prioridad = null;
        for (String parametro : consulta.split("&")) {
            int igual = parametro.indexOf('=');
            if (igual < 0) {
                continue;
            }
            String nombre = URLDecoder.decode(parametro.substring(0, igual), StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(parametro.substring(igual + 1), StandardCharsets.UTF_8).trim();
            if (nombre.equals("prioridad") && !valor.isEmpty()) {
                prioridad = Prioridad.valueOf(valor);
            }
        }
        return prioridad;
    }

    //El identificador de la solicitud debe ser un numero positivo
    private static long leerId(String texto) {
        long id = Long.parseLong(texto);
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador debe ser positivo");
        }
        return id;
    }

    //Usuario provisional de las cabeceras X-Usuario y X-Rol (D-023); null si faltan o no son validas
    private static Usuario usuario(HttpExchange exchange) {
        String nombre = exchange.getRequestHeaders().getFirst("X-Usuario");
        String rol = exchange.getRequestHeaders().getFirst("X-Rol");
        if (nombre == null || nombre.isBlank() || rol == null) {
            return null;
        }
        try {
            return new Usuario(nombre, Rol.valueOf(rol));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String error(String codigo) {
        return "{\"error\":\"" + codigo + "\"}";
    }

    private static void responder(HttpExchange exchange, int estado, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(estado, bytes.length);
        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    //Fechas en formato ISO, como en el resto de rutas (D-022)
    private static final class AdaptadorFecha
            implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        @Override
        public JsonElement serialize(LocalDateTime fecha, Type tipo, JsonSerializationContext contexto) {
            return new JsonPrimitive(fecha.toString());
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type tipo, JsonDeserializationContext contexto) {
            try {
                return LocalDateTime.parse(json.getAsString());
            } catch (DateTimeException | IllegalStateException e) {
                throw new JsonParseException("Fecha no valida", e);
            }
        }
    }
}
