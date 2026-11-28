package cita;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

public class CitasRealizacionServiceHttp
        implements CitasRealizacionService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>)
                    (json, tipo, contexto) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private final HttpClient http;
    private final String urlBase;

    public CitasRealizacionServiceHttp(String urlBase) {
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

    @Override
    public Cita marcarRealizada(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoRealizableException {
        if (citaId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de cita debe ser positivo");
        }

        HttpRequest request = prepararPeticion(
                solicitante, "/citas/" + citaId + "/realizar")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = enviar(request);
        comprobarPermiso(response);

        if (response.statusCode() == 404) {
            throw new CitaNoEncontradaException(
                    mensajeError(response));
        }
        if (response.statusCode() == 409) {
            throw new CitaNoRealizableException(
                    mensajeError(response));
        }
        if (response.statusCode() != 200) {
            comprobarOtrosErrores(response);
        }
        return leerCita(response.body());
    }

    private HttpRequest.Builder prepararPeticion(
            Usuario usuario,
            String ruta) throws SinPermisoException {
        if (usuario == null
                || usuario.nombre() == null
                || usuario.nombre().isBlank()
                || usuario.rol() != Rol.MEDICO
                || usuario.medicoId() == null
                || usuario.medicoId() <= 0) {
            throw new SinPermisoException(
                    "Se requiere un médico con identificador válido");
        }

        return HttpRequest.newBuilder(URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", usuario.nombre())
                .header("X-Rol", usuario.rol().name())
                .header("X-Medico-Id", usuario.medicoId().toString());
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
                    "La petición fue interrumpida", e);
        }
    }

    private void comprobarPermiso(
            HttpResponse<String> response)
            throws SinPermisoException {
        if (response.statusCode() == 403) {
            throw new SinPermisoException(mensajeError(response));
        }
    }

    private void comprobarOtrosErrores(
            HttpResponse<String> response) {
        if (response.statusCode() == 400) {
            throw new IllegalArgumentException(mensajeError(response));
        }
        throw new ServicioNoDisponibleException(
                "Respuesta inesperada del servidor: "
                        + response.statusCode());
    }

    private String mensajeError(HttpResponse<String> response) {
        try {
            var json = JsonParser.parseString(response.body())
                    .getAsJsonObject();
            if (json.has("mensaje")
                    && !json.get("mensaje").isJsonNull()) {
                return json.get("mensaje").getAsString();
            }
        } catch (RuntimeException e) {
            // Si el servidor no devuelve JSON, se informa el código HTTP.
        }
        return "Error HTTP " + response.statusCode();
    }

    private Cita leerCita(String cuerpo) {
        try {
            Cita cita = GSON.fromJson(cuerpo, Cita.class);
            if (cita == null
                    || cita.getId() == null
                    || cita.getEstado() == null
                    || cita.getFechaHora() == null) {
                throw new JsonParseException(
                        "Datos de cita incompletos");
            }
            return cita;
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta de cita no es válida", e);
        }
    }
}
