package agenda;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonParser;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

public class HorarioServicioServiceHttp
        implements HorarioServicioService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalTime.class,
                    new AdaptadorHora())
            .registerTypeAdapter(
                    LocalDateTime.class,
                    new AdaptadorFechaHora())
            .create();

    private final HttpClient http;
    private final String urlBase;

    public HorarioServicioServiceHttp(String urlBase) {
        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalArgumentException(
                    "La direccion del servidor es obligatoria");
        }
        String direccion = urlBase.trim();
        this.urlBase = direccion.endsWith("/")
                ? direccion.substring(0, direccion.length() - 1)
                : direccion;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public HorarioServicio consultar(Usuario solicitante)
            throws SinPermisoException {
        HttpRequest request = prepararPeticion(
                solicitante)
                .GET()
                .build();
        return leerRespuesta(enviar(request));
    }

    @Override
    public HorarioServicio modificar(
            Usuario solicitante,
            HorarioServicio horario)
            throws SinPermisoException {
        if (horario == null) {
            throw new IllegalArgumentException(
                    "El horario es obligatorio");
        }
        HttpRequest request = prepararPeticion(
                solicitante)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        GSON.toJson(horario)))
                .build();
        return leerRespuesta(enviar(request));
    }

    private HttpRequest.Builder prepararPeticion(
            Usuario solicitante) throws SinPermisoException {
        if (solicitante == null
                || solicitante.nombre() == null
                || solicitante.nombre().isBlank()
                || solicitante.rol() != Rol.ADMINISTRADOR) {
            throw new SinPermisoException(
                    "Se requiere un Administrador del Sistema");
        }
        return HttpRequest.newBuilder(
                    URI.create(urlBase + "/horarios-servicio"))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", solicitante.nombre())
                .header("X-Rol", solicitante.rol().name());
    }

    private HttpResponse<String> enviar(HttpRequest request) {
        try {
            return http.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ServicioNoDisponibleException(
                    "No se pudo conectar con el servidor", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException(
                    "La peticion fue interrumpida", e);
        }
    }

    private HorarioServicio leerRespuesta(
            HttpResponse<String> respuesta)
            throws SinPermisoException {
        if (respuesta.statusCode() == 403) {
            throw new SinPermisoException(
                    mensajeError(respuesta));
        }
        if (respuesta.statusCode() == 400) {
            throw new IllegalArgumentException(
                    mensajeError(respuesta));
        }
        if (respuesta.statusCode() != 200) {
            throw new ServicioNoDisponibleException(
                    "Respuesta inesperada del servidor: "
                            + respuesta.statusCode());
        }
        try {
            HorarioServicio horario = GSON.fromJson(
                    respuesta.body(),
                    HorarioServicio.class);
            if (horario == null || horario.getDias() == null
                    || horario.getDias().size() != 7
                    || horario.getDias().contains(null)) {
                throw new JsonParseException(
                        "Horario incompleto");
            }
            return horario;
        } catch (JsonParseException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta del horario no es valida", e);
        }
    }

    private String mensajeError(
            HttpResponse<String> respuesta) {
        try {
            var json = JsonParser.parseString(
                    respuesta.body()).getAsJsonObject();
            if (json.has("mensaje")
                    && !json.get("mensaje").isJsonNull()) {
                return json.get("mensaje").getAsString();
            }
        } catch (JsonParseException
                | IllegalStateException e) {
            return "Error HTTP " + respuesta.statusCode();
        }
        return "Error HTTP " + respuesta.statusCode();
    }

    private static final class AdaptadorHora
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
                        "Hora no valida", e);
            }
        }
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
                        "Fecha y hora no validas", e);
            }
        }
    }
}
