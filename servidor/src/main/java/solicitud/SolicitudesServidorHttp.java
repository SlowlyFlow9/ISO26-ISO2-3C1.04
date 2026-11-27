package solicitud;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
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
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * Comunicacion HTTP de CU11, CU12, CU13 y CU27. Utiliza JSON siguiendo D-022.
 * CU13 (D-037): GET /solicitudes con filtros, PUT /solicitudes/{id} y POST /solicitudes/{id}/anulacion.
 */
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

            } catch (SolicitudNoGestionableException e) {
                //CU13: la solicitud ya no esta pendiente de citacion (D-037)
                responder(exchange, 409,
                        error("ESTADO_NO_PERMITIDO"));

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

    //Registra la consulta de la lista de espera de CU27.
    public static void registrarListaEspera(
            HttpServer servidor,
            ListaEsperaService servicio) {

        servidor.createContext("/listas-espera", exchange -> {

            try {
                if (!exchange.getRequestURI().getPath()
                        .equals("/listas-espera")) {
                    responder(exchange, 404, error("NO_ENCONTRADO"));
                    return;
                }

                if (!exchange.getRequestMethod().equals("GET")) {
                    responder(exchange, 405,
                            error("METODO_NO_PERMITIDO"));
                    return;
                }

                Prioridad prioridad = leerPrioridadListaEspera(
                        exchange.getRequestURI().getRawQuery());

                responder(exchange, 200,
                        GSON.toJson(servicio.consultar(
                                usuario(exchange), prioridad)));

            } catch (SinPermisoException e) {
                responder(exchange, 403, error("SIN_PERMISO"));

            } catch (IllegalArgumentException e) {
                responder(exchange, 400, error("DATOS_INVALIDOS"));

            } catch (RuntimeException e) {
                responder(exchange, 500, error("ERROR_INTERNO"));
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

        // CU13: listar solicitudes con filtros (GET /solicitudes?pacienteId=&estado=&prioridad=)
        if (ruta.equals("/solicitudes") && metodo.equals("GET")) {

            FiltroSolicitudes filtro = leerFiltro(
                    exchange.getRequestURI().getRawQuery());

            responder(exchange, 200,
                    GSON.toJson(servicio.consultarSolicitudes(
                            usuario(exchange), filtro)));
            return;
        }

        // CU13: anular una solicitud (POST /solicitudes/{id}/anulacion)
        if (ruta.startsWith("/solicitudes/")
                && ruta.endsWith("/anulacion")) {

            if (!metodo.equals("POST")) {
                responder(exchange, 405,
                        error("METODO_NO_PERMITIDO"));
                return;
            }

            long id = Long.parseLong(ruta.substring(
                    "/solicitudes/".length(),
                    ruta.length() - "/anulacion".length()));

            SolicitudPrueba anulada = servicio.anularSolicitud(
                    usuario(exchange), id,
                    leerMotivo(leerCuerpo(exchange)));

            responder(exchange, 200,
                    GSON.toJson(anulada));
            return;
        }

        // CU13: modificar una solicitud (PUT /solicitudes/{id})
        if (ruta.startsWith("/solicitudes/") && metodo.equals("PUT")) {

            long id = Long.parseLong(ruta.substring(
                    "/solicitudes/".length()));

            DatosModificacionSolicitud datos = GSON.fromJson(
                    leerCuerpo(exchange),
                    DatosModificacionSolicitud.class);

            SolicitudPrueba modificada = servicio.modificarSolicitud(
                    usuario(exchange), id, datos);

            responder(exchange, 200,
                    GSON.toJson(modificada));
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

    //Recupera el usuario provisional de las cabeceras HTTP. Devuelve null si no hay usuario o rol
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

    //CU13: lee los filtros opcionales de la consulta; un valor no valido produce un 400
    static FiltroSolicitudes leerFiltro(String consulta) {

        Long pacienteId = null;
        EstadoSolicitud estado = null;
        Prioridad prioridad = null;

        if (consulta == null || consulta.isBlank()) {
            return FiltroSolicitudes.vacio();
        }

        for (String parametro : consulta.split("&")) {

            int igual = parametro.indexOf('=');
            if (igual < 0) {
                continue;
            }

            String nombre = URLDecoder.decode(
                    parametro.substring(0, igual), StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(
                    parametro.substring(igual + 1), StandardCharsets.UTF_8).trim();

            if (valor.isEmpty()) {
                continue;
            }

            switch (nombre) {
                case "pacienteId" -> {
                    pacienteId = Long.parseLong(valor);
                    if (pacienteId <= 0) {
                        throw new IllegalArgumentException(
                            "El paciente debe ser un identificador positivo");
                    }
                }
                case "estado" -> estado = EstadoSolicitud.valueOf(valor);
                case "prioridad" -> prioridad = Prioridad.valueOf(valor);
                default -> { }
            }
        }

        return new FiltroSolicitudes(pacienteId, estado, prioridad);
    }

    //CU27: lee el filtro opcional de prioridad de la lista de espera.
    private static Prioridad leerPrioridadListaEspera(String consulta) {

        if (consulta == null || consulta.isBlank()) {
            return null;
        }

        Prioridad prioridad = null;
        for (String parametro : consulta.split("&")) {

            int igual = parametro.indexOf('=');
            if (igual < 0) {
                continue;
            }

            String nombre = URLDecoder.decode(
                    parametro.substring(0, igual),
                    StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(
                    parametro.substring(igual + 1),
                    StandardCharsets.UTF_8).trim();

            if (nombre.equals("prioridad") && !valor.isEmpty()) {
                prioridad = Prioridad.valueOf(valor);
            }
        }

        return prioridad;
    }

    //CU13: lee el motivo de la anulacion del cuerpo {"motivo": "..."}
    private static String leerMotivo(String cuerpo) {

        JsonElement json = JsonParser.parseString(cuerpo);

        if (!json.isJsonObject()
                || !json.getAsJsonObject().has("motivo")
                || !json.getAsJsonObject().get("motivo").isJsonPrimitive()) {
            throw new IllegalArgumentException(
                "Falta el motivo de la anulacion");
        }

        return json.getAsJsonObject().get("motivo").getAsString();
    }

    //Lee el cuerpo de la peticion como texto
    private static String leerCuerpo(
            HttpExchange exchange) throws IOException {

        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
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
