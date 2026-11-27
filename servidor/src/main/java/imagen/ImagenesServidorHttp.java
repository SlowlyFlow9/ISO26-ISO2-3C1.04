package imagen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

public final class ImagenesServidorHttp {

    private static final int MAX_CUERPO_HTTP =
            8 * 1024 * 1024;

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

            } catch (DatosImagenInvalidosException
                    | IllegalArgumentException
                    | JsonParseException e) {

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
            SolicitudImagenNoEncontradaException,
            DatosImagenInvalidosException {

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

        String metodo = exchange.getRequestMethod();

        Usuario usuario = usuario(exchange);

        // CU18 - Crear una imagen.
        if (listado && metodo.equals("POST")) {

            crearImagen(exchange, servicio, usuario);

            return;
        }

        if (!metodo.equals("GET")) {

            exchange.getResponseHeaders().set(
                    "Allow",
                    listado ? "GET, POST" : "GET");

            responder(exchange, 405,
                    error("METODO_NO_PERMITIDO",
                            "Método no permitido"));

            return;
        }

        // CU17 - Consultar listado.
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

            // CU17 - Consultar una imagen.
            long imagenId = Long.parseLong(
                    ruta.substring("/imagenes/".length()));

            responder(exchange, 200,
                    GSON.toJson(
                            servicio.consultar(usuario, imagenId)));
        }
    }

    private static void crearImagen(
            HttpExchange exchange,
            ImagenesService servicio,
            Usuario usuario)
            throws IOException,
            SinPermisoException,
            SolicitudImagenNoEncontradaException,
            DatosImagenInvalidosException {

        ControlAcceso.exigir(usuario, Rol.MEDICO);

        if (usuario.medicoId() == null
                || usuario.medicoId() <= 0) {

            throw new SinPermisoException(
                    "El médico debe estar identificado");
        }

        String tipo = exchange.getRequestHeaders()
                .getFirst("Content-Type");

        if (tipo == null
                || !tipo.split(";", 2)[0].trim()
                        .equalsIgnoreCase("application/json")) {

            responder(exchange, 415,
                    error("FORMATO_NO_ADMITIDO",
                            "Se requiere application/json"));

            return;
        }

        byte[] bytes = exchange.getRequestBody()
                .readNBytes(MAX_CUERPO_HTTP + 1);

        if (bytes.length > MAX_CUERPO_HTTP) {

            responder(exchange, 413,
                    error("PETICION_DEMASIADO_GRANDE",
                            "La petición supera el tamaño permitido"));

            return;
        }

        String cuerpo = new String(
                bytes, StandardCharsets.UTF_8);

        DatosCreacionImagen datos = GSON.fromJson(
                cuerpo,
                DatosCreacionImagen.class);

        ResumenImagenPrueba creada =
                servicio.crear(usuario, datos);

        exchange.getResponseHeaders().set(
                "Location",
                "/imagenes/" + creada.id());

        responder(exchange, 201, GSON.toJson(creada));
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