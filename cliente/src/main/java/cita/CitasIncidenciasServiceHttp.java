package cita;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

public class CitasIncidenciasServiceHttp
        implements CitasIncidenciasService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>)
                    (json, tipo, contexto) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private static final Type LISTA_INCIDENCIAS =
            new TypeToken<List<IncidenciaCita>>() { }.getType();

    private final HttpClient http;
    private final String urlBase;

    public CitasIncidenciasServiceHttp(String urlBase) {
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
    public IncidenciaCita registrarIncidencia(
            Usuario solicitante,
            long citaId,
            String descripcion)
            throws SinPermisoException,
            CitaNoEncontradaException,
            CitaNoIncidenciableException {
        validarCitaId(citaId);
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                    "La descripción de la incidencia es obligatoria");
        }

        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("descripcion", descripcion);
        HttpRequest request = prepararPeticion(
                    solicitante, citaId)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        GSON.toJson(cuerpo)))
                .build();

        HttpResponse<String> response = enviar(request);
        comprobarPermiso(response);
        if (response.statusCode() == 404) {
            throw new CitaNoEncontradaException(
                    mensajeError(response));
        }
        if (response.statusCode() == 409) {
            throw new CitaNoIncidenciableException(
                    mensajeError(response));
        }
        if (response.statusCode() != 201) {
            comprobarOtrosErrores(response);
        }
        return leerIncidencia(response.body());
    }

    @Override
    public List<IncidenciaCita> listarIncidencias(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            CitaNoEncontradaException {
        validarCitaId(citaId);
        HttpRequest request = prepararPeticion(
                    solicitante, citaId)
                .GET()
                .build();

        HttpResponse<String> response = enviar(request);
        comprobarPermiso(response);
        if (response.statusCode() == 404) {
            throw new CitaNoEncontradaException(
                    mensajeError(response));
        }
        if (response.statusCode() != 200) {
            comprobarOtrosErrores(response);
        }

        try {
            List<IncidenciaCita> resultado =
                    GSON.fromJson(response.body(), LISTA_INCIDENCIAS);
            if (resultado == null || resultado.contains(null)) {
                throw new JsonParseException(
                        "Listado de incidencias no válido");
            }
            for (IncidenciaCita incidencia : resultado) {
                validarIncidencia(incidencia);
            }
            return resultado;
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "El historial de incidencias no es válido", e);
        }
    }

    private HttpRequest.Builder prepararPeticion(
            Usuario usuario,
            long citaId) throws SinPermisoException {
        if (usuario == null
                || usuario.nombre() == null
                || usuario.nombre().isBlank()
                || usuario.rol() != Rol.MEDICO
                || usuario.medicoId() == null
                || usuario.medicoId() <= 0) {
            throw new SinPermisoException(
                    "Se requiere un médico con identificador válido");
        }

        return HttpRequest.newBuilder(
                    URI.create(urlBase + "/citas/" + citaId
                            + "/incidencias"))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", usuario.nombre())
                .header("X-Rol", usuario.rol().name())
                .header("X-Medico-Id",
                        usuario.medicoId().toString());
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
            return "Error HTTP " + response.statusCode();
        }
        return "Error HTTP " + response.statusCode();
    }

    private IncidenciaCita leerIncidencia(String cuerpo) {
        try {
            IncidenciaCita incidencia =
                    GSON.fromJson(cuerpo, IncidenciaCita.class);
            validarIncidencia(incidencia);
            return incidencia;
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta de incidencia no es válida", e);
        }
    }

    private static void validarIncidencia(
            IncidenciaCita incidencia) {
        if (incidencia == null
                || incidencia.getId() == null
                || incidencia.getId() <= 0
                || incidencia.getCitaId() <= 0
                || incidencia.getMedicoId() <= 0
                || incidencia.getFechaHora() == null
                || incidencia.getDescripcion() == null
                || incidencia.getDescripcion().isBlank()) {
            throw new JsonParseException(
                    "Faltan datos de la incidencia");
        }
    }

    private static void validarCitaId(long citaId) {
        if (citaId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de cita debe ser positivo");
        }
    }
}
