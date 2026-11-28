package cita;

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
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import paciente.SinPermisoException;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

/**
 * CU06: Consulta HTTP del historial de pruebas.
 */

public class HistorialPruebasServiceHttp
        implements HistorialPruebasService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>)
                    (json, tipo, contexto) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private final String urlBase;
    private final HttpClient http;

    public HistorialPruebasServiceHttp(String urlBase) {

        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalArgumentException(
                    "La direccion del servidor es obligatoria");
        }

        String direccion = urlBase.trim();

        this.urlBase = direccion.endsWith("/")
                ? direccion.substring(0, direccion.length() - 1)
                : direccion;

        http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public List<Cita> listarMiHistorial(
            Usuario solicitante)
            throws SinPermisoException,
                   PacienteNoVinculadoException {

        if (solicitante == null
                || solicitante.nombre() == null
                || solicitante.nombre().isBlank()
                || solicitante.rol() == null) {

            throw new SinPermisoException(
                    "El usuario no es valido");
        }

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(
                        urlBase + "/historial-pruebas"))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", solicitante.nombre())
                .header("X-Rol", solicitante.rol().name())
                .GET()
                .build();

        HttpResponse<String> respuesta;

        try {
            respuesta = http.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString());

        } catch (IOException e) {
            throw new ServicioNoDisponibleException(
                    "No se pudo conectar con el servidor", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new ServicioNoDisponibleException(
                    "La peticion fue interrumpida", e);
        }

        if (respuesta.statusCode() == 403) {
            throw new SinPermisoException(
                    mensajeError(respuesta.body()));
        }

        if (respuesta.statusCode() != 200) {
            throw new ServicioNoDisponibleException(
                    "Error del servidor: "
                    + respuesta.statusCode());
        }

        try {
            List<Cita> historial = GSON.fromJson(
                    respuesta.body(),
                    new TypeToken<List<Cita>>() {}
                            .getType());

            if (historial == null
                    || historial.contains(null)) {
                throw new IllegalArgumentException(
                        "Listado de historial no valido");
            }

            for (Cita cita : historial) {

                if (cita.getId() == null
                        || cita.getFechaHora() == null
                        || cita.getEstado() != EstadoCita.REALIZADA) {

                    throw new IllegalArgumentException(
                            "Datos del historial incompletos");
                }
            }

            return historial;

        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta del historial no es valida", e);
        }
    }

    private String mensajeError(String cuerpo) {

        try {
            var json = JsonParser.parseString(
                    cuerpo).getAsJsonObject();

            if (json.has("mensaje")
                    && !json.get("mensaje").isJsonNull()) {

                return json.get("mensaje").getAsString();
            }

        } catch (RuntimeException e) {
            //Si no hay mensaje JSON, usamos uno general.
        }

        return "No tienes permiso para consultar el historial";
    }
}
