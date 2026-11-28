package cita;

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

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU06: Consulta HTTP del historial de pruebas.
 */

public final class HistorialPruebasServidorHttp {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>)
                    (fecha, tipo, contexto) ->
                            new JsonPrimitive(fecha.toString()))
            .create();

    private HistorialPruebasServidorHttp() {
    }

    public static void registrar(
            HttpServer servidor,
            HistorialPruebasService servicio) {

        if (servidor == null || servicio == null) {
            throw new IllegalArgumentException(
                    "Servidor y servicio obligatorios");
        }

        servidor.createContext("/historial-pruebas", exchange -> {
            try {
                atender(exchange, servicio);

            } catch (SinPermisoException
                    | PacienteNoVinculadoException e) {

                responder(exchange, 403,
                        error("SIN_PERMISO", e.getMessage()));

            } catch (RuntimeException e) {

                responder(exchange, 500,
                        error("ERROR_INTERNO",
                                "Error interno del servidor"));
            }
        });
    }

    private static void atender(
            HttpExchange exchange,
            HistorialPruebasService servicio)
            throws IOException, SinPermisoException,
                   PacienteNoVinculadoException {

        String ruta = exchange.getRequestURI().getPath();

        if (!ruta.equals("/historial-pruebas")) {
            responder(exchange, 404,
                    error("NO_ENCONTRADA", null));
            return;
        }

        if (!exchange.getRequestMethod().equals("GET")) {
            responder(exchange, 405,
                    error("METODO_NO_PERMITIDO", null));
            return;
        }

        Usuario usuario = leerUsuario(exchange);

        responder(exchange, 200,
                GSON.toJson(
                        servicio.listarMiHistorial(usuario)));
    }

    private static Usuario leerUsuario(
            HttpExchange exchange) {

        String nombre = exchange.getRequestHeaders()
                .getFirst("X-Usuario");

        String rol = exchange.getRequestHeaders()
                .getFirst("X-Rol");

        if (nombre == null || nombre.isBlank()
                || rol == null) {
            return null;
        }

        try {
            return new Usuario(nombre, Rol.valueOf(rol));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String error(
            String codigo, String mensaje) {

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

        byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8");

        exchange.sendResponseHeaders(estado, datos.length);

        try (var salida = exchange.getResponseBody()) {
            salida.write(datos);
        }
    }
}
