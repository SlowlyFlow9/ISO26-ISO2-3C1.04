package notificacion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import cita.PacienteNoVinculadoException;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public final class NotificacionesServidorHttp {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>)
                    (fecha, tipo, contexto) ->
                            new JsonPrimitive(fecha.toString()))
            .create();

    private NotificacionesServidorHttp() {
    }

    public static void registrar(
            HttpServer servidor,
            NotificacionesService servicio) {
        if (servidor == null || servicio == null) {
            throw new IllegalArgumentException(
                    "Servidor y servicio obligatorios");
        }

        servidor.createContext("/notificaciones", exchange -> {
            try {
                atender(exchange, servicio);
            } catch (SinPermisoException
                    | PacienteNoVinculadoException e) {
                responder(
                        exchange,
                        403,
                        error("SIN_PERMISO", e.getMessage()));
            }
        });
    }

    private static void atender(
            HttpExchange exchange,
            NotificacionesService servicio)
            throws IOException,
            SinPermisoException,
            PacienteNoVinculadoException {
        if (!exchange.getRequestURI().getPath()
                .equals("/notificaciones")) {
            responder(
                    exchange,
                    404,
                    error("RUTA_NO_ENCONTRADA", null));
            return;
        }

        if (!exchange.getRequestMethod().equals("GET")) {
            responder(
                    exchange,
                    405,
                    error("METODO_NO_PERMITIDO", null));
            return;
        }

        Usuario solicitante = leerUsuario(exchange);
        String cuerpo = GSON.toJson(
                servicio.listarMisNotificaciones(solicitante));
        responder(exchange, 200, cuerpo);
    }

    private static Usuario leerUsuario(HttpExchange exchange) {
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
                "Content-Type",
                "application/json; charset=utf-8");
        exchange.sendResponseHeaders(estado, bytes.length);

        try (var salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
