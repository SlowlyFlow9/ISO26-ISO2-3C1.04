package solicitud;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

public class ListaEsperaServiceHttp implements ListaEsperaService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>)
                    (fecha, tipo, contexto) ->
                            new JsonPrimitive(fecha.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>)
                    (json, tipo, contexto) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private final HttpClient http;
    private final String urlBase;

    public ListaEsperaServiceHttp(String urlBase) {
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
    public List<EntradaListaEspera> consultar(
            Usuario solicitante,
            Prioridad prioridad) {

        String ruta = "/listas-espera";
        if (prioridad != null) {
            ruta += "?prioridad=" + URLEncoder.encode(
                    prioridad.name(), StandardCharsets.UTF_8);
        }

        HttpRequest peticion = prepararPeticion(solicitante, ruta)
                .GET()
                .build();

        HttpResponse<String> respuesta = enviar(peticion);
        comprobarRespuesta(respuesta);

        try {
            List<EntradaListaEspera> entradas = GSON.fromJson(
                    respuesta.body(),
                    new TypeToken<List<EntradaListaEspera>>() { }.getType());

            if (entradas == null) {
                throw new IllegalStateException(
                        "El listado recibido es nulo");
            }

            for (EntradaListaEspera entrada : entradas) {
                if (entrada == null
                        || entrada.solicitudId() <= 0
                        || entrada.pacienteId() <= 0
                        || entrada.tipoPruebaId() <= 0
                        || entrada.prioridad() == null) {
                    throw new IllegalStateException(
                            "El listado recibido está incompleto");
                }
            }

            return List.copyOf(entradas);

        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta de la lista de espera no es válida", e);
        }
    }

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

        return HttpRequest.newBuilder(URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", solicitante.nombre())
                .header("X-Rol", solicitante.rol().name());
    }

    private HttpResponse<String> enviar(HttpRequest peticion) {
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

    private static void comprobarRespuesta(
            HttpResponse<String> respuesta) {

        int codigo = respuesta.statusCode();
        if (codigo == 200) {
            return;
        }

        switch (codigo) {
            case 400 -> throw new IllegalArgumentException(
                    "El filtro de prioridad no es válido.");
            case 403 -> throw new SecurityException(
                    "No tienes permiso para consultar la lista de espera.");
            case 404 -> throw new NoSuchElementException(
                    "No se ha encontrado la lista de espera.");
            default -> throw new ServicioNoDisponibleException(
                    "No se pudo consultar la lista de espera. "
                    + "Error HTTP: " + codigo);
        }
    }
}
