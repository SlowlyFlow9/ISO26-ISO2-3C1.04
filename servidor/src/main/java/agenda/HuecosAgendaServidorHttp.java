package agenda;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

/**
 * CU29 - Comunicación HTTP para gestionar huecos.
 *
 * Utiliza HTTP/JSON siguiendo las decisiones D-022 y D-023 del proyecto.
 */
public class HuecosAgendaServidorHttp {

    // Convierte horas Java a JSON y viceversa.
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
                    LocalTime.class,
                    new AdaptadorHora())
            .create();

    private HuecosAgendaServidorHttp() {
    }

    // Registra las rutas de CU29 en el servidor común.
    public static void registrar(
            HttpServer servidor,
            HuecosAgendaService servicio) {

        if (servidor == null || servicio == null) {
            throw new IllegalArgumentException(
                    "El servidor y el servicio son obligatorios");
        }

        servidor.createContext("/huecos", exchange -> {

            try {
                atender(exchange, servicio);

            } catch (SinPermisoException e) {
                responder(exchange, 403,
                        error("SIN_PERMISO", e.getMessage()));

            } catch (AgendaNoEncontradaException
                    | HuecoAgendaNoEncontradoException e) {

                responder(exchange, 404,
                        error("NO_ENCONTRADO", e.getMessage()));

            } catch (DatosHuecoInvalidosException e) {
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

    // Identifica y ejecuta la operación solicitada.
    private static void atender(
            HttpExchange exchange,
            HuecosAgendaService servicio)
            throws IOException, SinPermisoException,
            AgendaNoEncontradaException,
            HuecoAgendaNoEncontradoException,
            DatosHuecoInvalidosException {

        String ruta = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();

        Usuario solicitante = usuario(exchange);

        // CREAR: POST /huecos
        if (ruta.equals("/huecos")
                && metodo.equals("POST")) {

            String cuerpo = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            HuecoAgenda datos = GSON.fromJson(
                    cuerpo, HuecoAgenda.class);

            HuecoAgenda creado = servicio.crear(
                    solicitante, datos);

            responder(exchange, 201,
                    GSON.toJson(creado));
            return;
        }

        // CONSULTAR: GET /huecos?agendaId=1
        if (ruta.equals("/huecos")
                && metodo.equals("GET")) {

            long agendaId = leerAgendaId(
                    exchange.getRequestURI().getRawQuery());

            List<HuecoAgenda> resultado =
                    servicio.listarPorAgenda(
                            solicitante, agendaId);

            responder(exchange, 200,
                    GSON.toJson(resultado));
            return;
        }

        // BLOQUEAR o LIBERAR: POST /huecos/{id}/operacion
        String[] partes = ruta.split("/");

        if (partes.length == 4
                && partes[1].equals("huecos")
                && (partes[3].equals("bloquear")
                    || partes[3].equals("liberar"))) {

            if (!metodo.equals("POST")) {
                responder(exchange, 405,
                        error("METODO_NO_PERMITIDO", null));
                return;
            }

            long huecoId = Long.parseLong(partes[2]);

            if (huecoId <= 0) {
                throw new IllegalArgumentException(
                        "El identificador debe ser positivo");
            }

            HuecoAgenda resultado;

            if (partes[3].equals("bloquear")) {
                resultado = servicio.bloquear(
                        solicitante, huecoId);
            } else {
                resultado = servicio.liberar(
                        solicitante, huecoId);
            }

            responder(exchange, 200,
                    GSON.toJson(resultado));
            return;
        }

        // Método no permitido para la ruta principal.
        if (ruta.equals("/huecos")) {
            responder(exchange, 405,
                    error("METODO_NO_PERMITIDO", null));
            return;
        }

        // Ruta desconocida.
        responder(exchange, 404,
                error("NO_ENCONTRADO", null));
    }

    // Recupera el identificador de la agenda de la URL.
    private static long leerAgendaId(String consulta) {

        if (consulta != null) {

            for (String parametro : consulta.split("&")) {

                String[] partes = parametro.split("=", 2);

                if (partes.length == 2
                        && URLDecoder.decode(
                                partes[0],
                                StandardCharsets.UTF_8)
                                .equals("agendaId")) {

                    long id = Long.parseLong(
                            URLDecoder.decode(
                                    partes[1],
                                    StandardCharsets.UTF_8));

                    if (id <= 0) {
                        throw new IllegalArgumentException(
                                "El ID de agenda debe ser positivo");
                    }

                    return id;
                }
            }
        }

        throw new IllegalArgumentException(
                "Falta el parámetro agendaId");
    }

    // Identifica al usuario mediante las cabeceras HTTP.
    private static Usuario usuario(HttpExchange exchange) {

        String nombre = exchange.getRequestHeaders()
                .getFirst("X-Usuario");

        String rol = exchange.getRequestHeaders()
                .getFirst("X-Rol");

        if (nombre == null || nombre.isBlank()
                || rol == null) {
            return null;
        }

        try {
            return new Usuario(
                    nombre, Rol.valueOf(rol));

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Genera un error en formato JSON.
    private static String error(
            String codigo, String mensaje) {

        JsonObject json = new JsonObject();
        json.addProperty("error", codigo);

        if (mensaje != null) {
            json.addProperty("mensaje", mensaje);
        }

        return GSON.toJson(json);
    }

    // Envía la respuesta HTTP al cliente.
    private static void responder(
            HttpExchange exchange,
            int estado,
            String cuerpo) throws IOException {

        byte[] bytes = cuerpo.getBytes(
                StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8");

        exchange.sendResponseHeaders(
                estado, bytes.length);

        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

}
