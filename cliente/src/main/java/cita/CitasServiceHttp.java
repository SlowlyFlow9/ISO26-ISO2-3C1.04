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
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import paciente.SinPermisoException;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

/**
 * CU01 y CU02 - Comunicacion HTTP de citas
 */
public class CitasServiceHttp implements CitasService {

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

    public CitasServiceHttp(String urlBase) {

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

    private HttpRequest.Builder prepararPeticion(
            Usuario usuario,
            String ruta) throws SinPermisoException {

        if (usuario == null
                || usuario.nombre() == null
                || usuario.nombre().isBlank()
                || usuario.rol() == null) {

            throw new SinPermisoException(
                    "El usuario no es valido");
        }

        return HttpRequest.newBuilder(
                    URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", usuario.nombre())
                .header("X-Rol", usuario.rol().name());
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
                    "La peticion fue interrumpida", e);
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
            // Si el error no contiene JSON valido,
            // usamos el codigo HTTP.
        }

        return "Error HTTP " + respuesta.statusCode();
    }

    private void comprobarPermiso(
            HttpResponse<String> respuesta)
            throws SinPermisoException {

        if (respuesta.statusCode() == 403) {
            throw new SinPermisoException(
                    mensajeError(respuesta));
        }
    }

    private void comprobarOtrosErrores(
            HttpResponse<String> respuesta) {

        if (respuesta.statusCode() == 400) {
            throw new IllegalArgumentException(
                    mensajeError(respuesta));
        }

        throw new ServicioNoDisponibleException(
                "Respuesta inesperada del servidor: "
                + respuesta.statusCode());
    }

    private Cita leerCita(String cuerpo) {

        try {
            Cita cita = GSON.fromJson(
                    cuerpo, Cita.class);

            if (cita == null
                    || cita.getId() == null
                    || cita.getEstado() == null
                    || cita.getFechaHora() == null) {

                throw new JsonParseException(
                        "Faltan datos de la cita");
            }

            return cita;

        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta de cita no es valida", e);
        }
    }

    @Override
    public List<Cita> listarMisCitas(
            Usuario solicitante)
            throws SinPermisoException,
            PacienteNoVinculadoException {

        HttpRequest peticion =
                prepararPeticion(
                        solicitante, "/citas")
                .GET()
                .build();

        HttpResponse<String> respuesta = enviar(peticion);

        comprobarPermiso(respuesta);

        if (respuesta.statusCode() != 200) {
            comprobarOtrosErrores(respuesta);
        }

        try {
            List<Cita> citas = GSON.fromJson(
                    respuesta.body(),
                    new TypeToken<List<Cita>>() {}
                            .getType());

            if (citas == null || citas.contains(null)) {
                throw new JsonParseException(
                        "Listado de citas no valido");
            }

            for (Cita cita : citas) {
                if (cita.getId() == null
                        || cita.getEstado() == null
                        || cita.getFechaHora() == null) {

                    throw new JsonParseException(
                            "Datos de cita incompletos");
                }
            }

            return citas;

        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "El listado de citas no es valido", e);
        }
    }

    @Override
    public Cita consultarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException {

        if (citaId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador debe ser positivo");
        }

        HttpRequest peticion =
                prepararPeticion(
                        solicitante, "/citas/" + citaId)
                .GET()
                .build();

        HttpResponse<String> respuesta = enviar(peticion);

        comprobarPermiso(respuesta);

        if (respuesta.statusCode() == 404) {
            throw new CitaNoEncontradaException(
                    mensajeError(respuesta));
        }

        if (respuesta.statusCode() != 200) {
            comprobarOtrosErrores(respuesta);
        }

        return leerCita(respuesta.body());
    }

    @Override
    public Cita confirmarMiCita(
            Usuario solicitante,
            long citaId)
            throws SinPermisoException,
            PacienteNoVinculadoException,
            CitaNoEncontradaException,
            CitaNoConfirmableException {

        if (citaId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador debe ser positivo");
        }

        HttpRequest peticion =
                prepararPeticion(
                        solicitante,
                        "/citas/" + citaId + "/confirmar")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> respuesta = enviar(peticion);

        comprobarPermiso(respuesta);

        if (respuesta.statusCode() == 404) {
            throw new CitaNoEncontradaException(
                    mensajeError(respuesta));
        }

        if (respuesta.statusCode() == 409) {
            throw new CitaNoConfirmableException(
                    mensajeError(respuesta));
        }

        if (respuesta.statusCode() != 200) {
            comprobarOtrosErrores(respuesta);
        }

        return leerCita(respuesta.body());
    }
}
