package notificacion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

public class NotificacionesServiceHttp
        implements NotificacionesService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>)
                    (json, tipo, contexto) ->
                            LocalDateTime.parse(
                                    json.getAsString()))
            .create();

    private final HttpClient http;
    private final String urlBase;

    public NotificacionesServiceHttp(String urlBase) {
        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalArgumentException(
                    "La dirección del servidor es obligatoria");
        }

        String direccion = urlBase.trim();
        this.urlBase = direccion.endsWith("/")
                ? direccion.substring(0, direccion.length() - 1)
                : direccion;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    private HttpRequest prepararPeticion(
            Usuario usuario) throws SinPermisoException {
        if (usuario == null
                || usuario.nombre() == null
                || usuario.nombre().isBlank()
                || usuario.rol() == null) {
            throw new SinPermisoException(
                    "El usuario no es válido");
        }

        return HttpRequest.newBuilder(
                    URI.create(urlBase + "/notificaciones"))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", usuario.nombre())
                .header("X-Rol", usuario.rol().name())
                .GET()
                .build();
    }

    private HttpResponse<String> enviar(
            HttpRequest peticion) {
        try {
            return http.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ServicioNoDisponibleException(
                    "No se pudo conectar con el servidor", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException(
                    "La petición fue interrumpida", e);
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
        } catch (RuntimeException e) {
            // Si el error no contiene JSON válido, se usa el código HTTP.
        }
        return "Error HTTP " + respuesta.statusCode();
    }

    private void comprobarRespuesta(
            HttpResponse<String> respuesta)
            throws SinPermisoException {
        if (respuesta.statusCode() == 403) {
            throw new SinPermisoException(
                    mensajeError(respuesta));
        }
        if (respuesta.statusCode() != 200) {
            throw new ServicioNoDisponibleException(
                    "Respuesta inesperada del servidor: "
                    + respuesta.statusCode());
        }
    }

    private List<Notificacion> leerNotificaciones(
            String cuerpo) {
        try {
            List<Notificacion> notificaciones = GSON.fromJson(
                    cuerpo,
                    new TypeToken<List<Notificacion>>() {}.getType());

            if (notificaciones == null
                    || notificaciones.contains(null)) {
                throw new JsonParseException(
                        "Listado de notificaciones no válido");
            }

            for (Notificacion notificacion : notificaciones) {
                if (notificacion.getId() == null
                        || notificacion.getId() <= 0
                        || notificacion.getTitulo() == null
                        || notificacion.getTitulo().isBlank()
                        || notificacion.getMensaje() == null
                        || notificacion.getFechaHora() == null
                        || notificacion.getLeida() == null) {
                    throw new JsonParseException(
                            "Datos de notificación incompletos");
                }
            }

            return notificaciones;
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "El listado de notificaciones no es válido", e);
        }
    }

    @Override
    public List<Notificacion> listarMisNotificaciones(
            Usuario solicitante)
            throws SinPermisoException {
        HttpRequest peticion = prepararPeticion(solicitante);
        HttpResponse<String> respuesta = enviar(peticion);
        comprobarRespuesta(respuesta);
        return leerNotificaciones(respuesta.body());
    }
}
