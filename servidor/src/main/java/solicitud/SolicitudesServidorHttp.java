package solicitud;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.DateTimeException;
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

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/** Comunicacion HTTP del CU11. Utiliza JSON siguiendo D-022. */
public class SolicitudesServidorHttp {

    // Adaptador para fechas en formato JSON
    private static class AdaptadorFecha
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
                return LocalDateTime.parse(
                        json.getAsString());

            } catch (DateTimeException | IllegalStateException e) {
                throw new JsonParseException(
                        "Fecha invalida", e);
            }
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    new AdaptadorFecha())
            .create();

    private SolicitudesServidorHttp() {
    }

    //Registra las rutas de solicitudes en un servidor HTTP existente.
    public static void registrar(
            HttpServer servidor,
            SolicitudesService servicio) {

        servidor.createContext("/solicitudes", exchange -> {

            try {
                atender(exchange, servicio);

            } catch (SinPermisoException e) {
                responder(exchange, 403,
                        error("SIN_PERMISO"));

            } catch (NoSuchElementException e) {
                responder(exchange, 404,
                        error("NO_ENCONTRADO"));

            } catch (IllegalArgumentException
                    | JsonParseException e) {
                responder(exchange, 400,
                        error("DATOS_INVALIDOS"));

            } catch (RuntimeException e) {
                responder(exchange, 500,
                        error("ERROR_INTERNO"));
            }
        });
    }

    //Atiende las peticiones del CU11.
    private static void atender(
            HttpExchange exchange,
            SolicitudesService servicio)
            throws IOException, SinPermisoException {

        String ruta =
                exchange.getRequestURI().getPath();

        String metodo =
                exchange.getRequestMethod();

        // 1. Consultar tipos de prueba disponibles
        if (ruta.equals("/solicitudes/tipos")) {

            if (!metodo.equals("GET")) {
                responder(exchange, 405,
                        error("METODO_NO_PERMITIDO"));
                return;
            }

            ControlAcceso.exigir(
                    usuario(exchange), Rol.MEDICO);

            responder(exchange, 200,
                    GSON.toJson(
                            servicio.consultarTiposPrueba()));
            return;
        }

        // 2. Crear una solicitud
        if (ruta.equals("/solicitudes")) {

            if (!metodo.equals("POST")) {
                responder(exchange, 405,
                        error("METODO_NO_PERMITIDO"));
                return;
            }

            Usuario medico = usuario(exchange);

            ControlAcceso.exigir(
                    medico, Rol.MEDICO);

            String cuerpo = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            SolicitudPrueba datos =
                    GSON.fromJson(
                            cuerpo,
                            SolicitudPrueba.class
                    );

            SolicitudPrueba creada =
                    servicio.crearSolicitud(medico, datos);

            responder(exchange, 201,
                    GSON.toJson(creada));

            return;
        }

        //3. Consultar una solicitud por identificador
        if (ruta.startsWith("/solicitudes/")) {

            if (!metodo.equals("GET")) {
                responder(exchange, 405,
                        error("METODO_NO_PERMITIDO"));
                return;
            }

            ControlAcceso.exigir(
                    usuario(exchange), Rol.MEDICO);

            String textoId = ruta.substring(
                    "/solicitudes/".length());

            long id = Long.parseLong(textoId);

            SolicitudPrueba solicitud =
                    servicio.consultarSolicitud(id)
                            .orElseThrow(
                                    NoSuchElementException::new);

            responder(exchange, 200,
                    GSON.toJson(solicitud));

            return;
        }

        responder(exchange, 404,
                error("NO_ENCONTRADO"));
    }

   
     //Recupera el usuario provisional de las cabeceras HTTP
    private static Usuario usuario(
            HttpExchange exchange) {

        String rol = exchange.getRequestHeaders()
                .getFirst("X-Rol");

        String nombre = exchange.getRequestHeaders()
                .getFirst("X-Usuario");

        if (rol == null || nombre == null
                || nombre.isBlank()) {
            return null;
        }

        try {
            return new Usuario(
                    nombre,
                    Rol.valueOf(rol)
            );

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

     //Genera una respuesta de error en JSON.
    private static String error(String codigo) {
        return "{\"error\":\"" + codigo + "\"}";
    }


     //Envia la respuesta HTTP.
    private static void responder(
            HttpExchange exchange,
            int estado,
            String cuerpo) throws IOException {

        byte[] bytes =
                cuerpo.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8"
        );

        exchange.sendResponseHeaders(
                estado, bytes.length);

        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
