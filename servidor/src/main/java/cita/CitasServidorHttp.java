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
 * CU01 y CU02 - Comunicacion HTTP de citas del paciente.
 *
 * GET  /citas
 * GET  /citas/{id}
 * POST /citas/{id}/confirmar
 * POST /citas/{id}/realizar
 */

public final class CitasServidorHttp {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>)
                    (fecha, tipo, contexto) ->
                            new JsonPrimitive(fecha.toString()))
            .create();

    private CitasServidorHttp() {
    }

    public static void registrar(
            HttpServer servidor,
            CitasService servicio) {

        registrar(servidor, servicio, null);
    }

    public static void registrar(
            HttpServer servidor,
            CitasService servicio,
            CitasRealizacionService servicioRealizacion) {

        if (servidor == null || servicio == null) {
            throw new IllegalArgumentException(
                    "Servidor y servicio obligatorios");
        }

        servidor.createContext("/citas", exchange -> {

            try {
                atender(exchange, servicio, servicioRealizacion);

            } catch (SinPermisoException
                    | PacienteNoVinculadoException e) {

                responder(exchange, 403,
                        error("SIN_PERMISO", e.getMessage()));

            } catch (CitaNoEncontradaException
                    | SolicitudNoEncontradaException e) {

                responder(exchange, 404,
                        error("NO_ENCONTRADA", e.getMessage()));

            } catch (CitaNoConfirmableException e) {

                responder(exchange, 409,
                        error("NO_CONFIRMABLE", e.getMessage()));

            } catch (CitaNoRealizableException e) {

                responder(exchange, 409,
                        error("NO_REALIZABLE", e.getMessage()));

            } catch (IllegalArgumentException e) {

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
            CitasService servicio,
            CitasRealizacionService servicioRealizacion)
            throws IOException, SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoConfirmableException,
            CitaNoRealizableException,
            SolicitudNoEncontradaException {

        String ruta =
                exchange.getRequestURI().getPath();

        String metodo =
                exchange.getRequestMethod();

        Usuario usuario = leerUsuario(exchange);

        // CU01 - Listar las citas propias.
        if (ruta.equals("/citas")) {

            if (!metodo.equals("GET")) {
                responder(exchange, 405,
                        error("METODO_NO_PERMITIDO", null));
                return;
            }

            responder(exchange, 200,
                    GSON.toJson(
                            servicio.listarMisCitas(usuario)));
            return;
        }

        String[] partes = ruta.split("/");

        if (partes.length >= 3
                && partes[1].equals("citas")) {

            long citaId = Long.parseLong(partes[2]);

            if (citaId <= 0) {
                throw new IllegalArgumentException(
                        "El ID de cita debe ser positivo");
            }

            //CU01: Consultar una cita propia.
            if (partes.length == 3) {

                if (!metodo.equals("GET")) {
                    responder(exchange, 405,
                            error("METODO_NO_PERMITIDO", null));
                    return;
                }

                responder(exchange, 200,
                        GSON.toJson(
                                servicio.consultarMiCita(
                                        usuario, citaId)));
                return;
            }

            if (partes.length == 4
                    && partes[3].equals("realizar")) {
                if (!metodo.equals("POST")) {
                    responder(exchange, 405,
                            error("METODO_NO_PERMITIDO", null));
                    return;
                }
                if (servicioRealizacion == null) {
                    responder(exchange, 404,
                            error("NO_ENCONTRADA", null));
                    return;
                }

                responder(exchange, 200,
                        GSON.toJson(
                                servicioRealizacion.marcarRealizada(
                                        usuario, citaId)));
                return;
            }

            //CU02: Confirmar una cita propia.
            if (partes.length == 4
                    && partes[3].equals("confirmar")) {

                if (!metodo.equals("POST")) {
                    responder(exchange, 405,
                            error("METODO_NO_PERMITIDO", null));
                    return;
                }

                Cita confirmada =
                        servicio.confirmarMiCita(
                                usuario, citaId);

                responder(exchange, 200,
                        GSON.toJson(confirmada));
                return;
            }
        }

        responder(exchange, 404,
                error("NO_ENCONTRADA", null));
    }

    private static Usuario leerUsuario(
            HttpExchange exchange) {

        String nombre = exchange.getRequestHeaders()
                .getFirst("X-Usuario");

        String rol = exchange.getRequestHeaders()
                .getFirst("X-Rol");
        String medicoId = exchange.getRequestHeaders()
                .getFirst("X-Medico-Id");

        if (nombre == null || nombre.isBlank()
                || rol == null) {
            return null;
        }

        try {
            Rol rolUsuario = Rol.valueOf(rol);
            if (medicoId == null) {
                return new Usuario(nombre, rolUsuario);
            }

            long id = Long.parseLong(medicoId);
            if (id <= 0) {
                return null;
            }

            return new Usuario(nombre, rolUsuario, id);

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

        return json.toString();
    }

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
