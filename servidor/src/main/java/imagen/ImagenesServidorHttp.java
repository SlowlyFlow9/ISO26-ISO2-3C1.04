package imagen;

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

public final class ImagenesServidorHttp {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>)
                    (fecha, tipo, contexto) ->
                            new JsonPrimitive(fecha.toString()))
            .create();

    private ImagenesServidorHttp() {
    }

    public static void registrar(
            HttpServer servidor,
            ImagenesService servicio) {

        servidor.createContext("/imagenes", exchange -> {

            try {
                atender(exchange, servicio);

            } catch (SinPermisoException e) {

                responder(exchange, 403,
                        error("SIN_PERMISO", e.getMessage()));

            } catch (SolicitudImagenNoEncontradaException e) {

                responder(exchange, 404,
                        error("SOLICITUD_NO_ENCONTRADA",
                                e.getMessage()));

            } catch (ImagenNoEncontradaException e) {

                responder(exchange, 404,
                        error("IMAGEN_NO_ENCONTRADA",
                                e.getMessage()));

            } catch (IllegalArgumentException e) {

                responder(exchange, 400,
                        error("DATOS_INVALIDOS",
                                e.getMessage()));

            } finally {
                exchange.close();
            }
        });
    }

    private static void atender(
            HttpExchange exchange,
            ImagenesService servicio)
            throws IOException,
            SinPermisoException,
            ImagenNoEncontradaException,
            SolicitudImagenNoEncontradaException {

        String ruta = exchange.getRequestURI().getPath();

        boolean listado = ruta.equals("/imagenes");

        boolean detalle =
                ruta.matches("/imagenes/[^/]+");

        if (!listado && !detalle) {

            responder(exchange, 404,
                    error("NO_ENCONTRADO",
                            "Ruta inexistente"));

            return;
        }

        if (!exchange.getRequestMethod().equals("GET")) {

            exchange.getResponseHeaders().set("Allow", "GET");

            responder(exchange, 405,
                    error("METODO_NO_PERMITIDO",
                            "Método no permitido"));

            return;
        }

        Usuario usuario = usuario(exchange);

        if (listado) {

            String consulta =
                    exchange.getRequestURI().getRawQuery();

            if (consulta == null
                    || !consulta.matches("solicitudId=[0-9]+")) {

                throw new IllegalArgumentException(
                        "Debe indicar un ID de solicitud válido");
            }

            long solicitudId = Long.parseLong(
                    consulta.substring("solicitudId=".length()));

            responder(exchange, 200,
                    GSON.toJson(
                            servicio.listar(usuario, solicitudId)));

        } else {

            long imagenId = Long.parseLong(
                    ruta.substring("/imagenes/".length()));

            responder(exchange, 200,
                    GSON.toJson(
                            servicio.consultar(usuario, imagenId)));
        }
    }

    private static Usuario usuario(HttpExchange exchange) {

        String nombre = exchange.getRequestHeaders()
                .getFirst("X-Usuario");

        String rol = exchange.getRequestHeaders()
                .getFirst("X-Rol");

        String medicoId = exchange.getRequestHeaders()
                .getFirst("X-Medico-Id");

        if (nombre == null || nombre.isBlank() || rol == null) {
            return null;
        }

        try {

            Long id = medicoId == null
                    ? null : Long.valueOf(medicoId);

            if (id != null && id <= 0) {
                return null;
            }

            return new Usuario(nombre, Rol.valueOf(rol), id);

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String error(
            String codigo,
            String mensaje) {

        JsonObject json = new JsonObject();

        json.addProperty("error", codigo);
        json.addProperty("mensaje", mensaje);

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
}