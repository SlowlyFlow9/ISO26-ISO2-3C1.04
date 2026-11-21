package agenda;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

/**
 * CU29 - Cliente HTTP para gestionar huecos de agenda.
 *
 * Comunica la interfaz Swing con el servidor
 * utilizando HTTP y JSON.
 */
public class HuecosAgendaServiceHttp
        implements HuecosAgendaService {

    // Convierte LocalTime a JSON y viceversa.
    private static class AdaptadorHora
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
                        "Hora no válida en la respuesta", e);
            }
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalTime.class,
                    new AdaptadorHora())
            .create();

    private final HttpClient http;
    private final String urlBase;

    // Constructor del cliente HTTP.
    public HuecosAgendaServiceHttp(String urlBase) {

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

    // Prepara una petición con la identidad del usuario.
    private HttpRequest.Builder prepararPeticion(
            Usuario solicitante,
            String ruta) {

        if (solicitante == null
                || solicitante.nombre() == null
                || solicitante.nombre().isBlank()
                || solicitante.rol() == null) {

            throw new IllegalArgumentException(
                    "El usuario no es válido");
        }

        return HttpRequest.newBuilder(
                    URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", solicitante.nombre())
                .header("X-Rol", solicitante.rol().name());
    }

    // Envía la petición al servidor.
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

    // Comprueba el código de respuesta HTTP.
    private void comprobarRespuesta(
            HttpResponse<String> respuesta,
            int codigoEsperado) {

        int codigo = respuesta.statusCode();

        if (codigo == codigoEsperado) {
            return;
        }

        switch (codigo) {

            case 400:
                throw new IllegalArgumentException(
                        "Los datos del hueco no son válidos "
                        + "o existe un conflicto de horario.");

            case 403:
                throw new SecurityException(
                        "No tienes permiso para gestionar huecos.");

            case 404:
                throw new NoSuchElementException(
                        "No se ha encontrado la agenda "
                        + "o el hueco solicitado.");

            case 409:
                throw new IllegalStateException(
                        "Existe un conflicto de disponibilidad.");

            default:
                throw new ServicioNoDisponibleException(
                        "Error del servidor. Código HTTP: "
                        + codigo);
        }
    }

    // Convierte la respuesta JSON en un HuecoAgenda.
    private HuecoAgenda leerHueco(String cuerpo) {

        try {
            HuecoAgenda hueco = GSON.fromJson(
                    cuerpo, HuecoAgenda.class);

            if (hueco == null || hueco.getId() == null
                    || hueco.getHoraInicio() == null
                    || hueco.getHoraFin() == null) {

                throw new IllegalStateException(
                        "Faltan datos del hueco");
            }

            return hueco;

        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta de hueco no es válida", e);
        }
    }

    @Override
    public HuecoAgenda crear(
            Usuario solicitante,
            HuecoAgenda datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos del hueco son obligatorios");
        }

        HttpRequest peticion =
                prepararPeticion(solicitante, "/huecos")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        GSON.toJson(datos)))
                .build();

        HttpResponse<String> respuesta = enviar(peticion);

        comprobarRespuesta(respuesta, 201);

        return leerHueco(respuesta.body());
    }

    @Override
    public List<HuecoAgenda> listarPorAgenda(
            Usuario solicitante,
            long agendaId) {

        if (agendaId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de agenda debe ser positivo");
        }

        HttpRequest peticion =
                prepararPeticion(
                        solicitante,
                        "/huecos?agendaId=" + agendaId)
                .GET()
                .build();

        HttpResponse<String> respuesta = enviar(peticion);

        comprobarRespuesta(respuesta, 200);

        try {
            List<HuecoAgenda> resultado =
                    GSON.fromJson(
                            respuesta.body(),
                            new TypeToken<List<HuecoAgenda>>() {}
                                    .getType());

            if (resultado == null) {
                throw new IllegalStateException(
                        "El listado recibido es nulo");
            }

            return resultado;

        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "El listado de huecos no es válido", e);
        }
    }

    @Override
    public HuecoAgenda bloquear(
            Usuario solicitante,
            long huecoId) {

        return cambiarDisponibilidad(
                solicitante, huecoId, "bloquear");
    }

    @Override
    public HuecoAgenda liberar(
            Usuario solicitante,
            long huecoId) {

        return cambiarDisponibilidad(
                solicitante, huecoId, "liberar");
    }

    // Envía una petición para bloquear o liberar un hueco.
    private HuecoAgenda cambiarDisponibilidad(
            Usuario solicitante,
            long huecoId,
            String operacion) {

        if (huecoId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del hueco debe ser positivo");
        }

        HttpRequest peticion =
                prepararPeticion(
                        solicitante,
                        "/huecos/" + huecoId + "/" + operacion)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> respuesta = enviar(peticion);

        comprobarRespuesta(respuesta, 200);

        return leerHueco(respuesta.body());
    }



}
