package solicitud;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import paciente.Usuario;
import paciente.ServicioNoDisponibleException;
import tipoprueba.TipoPrueba;

/**
 * Cliente HTTP para prescribir pruebas diagnosticas.
 * Se comunica con el servidor usando JSON.
 */
public class SolicitudesServiceHttp implements SolicitudesService {

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

    //Constructor
    public SolicitudesServiceHttp(String urlBase) {

        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalArgumentException(
                "La direccion del servidor es obligatoria");
        }

        this.urlBase = urlBase.endsWith("/")
                ? urlBase.substring(0, urlBase.length() - 1)
                : urlBase;

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    //Construye la peticion con los datos del medico
    private HttpRequest.Builder prepararPeticion(
            Usuario medico, String ruta) {

        if (medico == null || medico.rol() == null
                || medico.nombre() == null
                || medico.nombre().isBlank()) {
            throw new IllegalArgumentException(
                "El usuario no es valido");
        }

        return HttpRequest.newBuilder(
                URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("X-Usuario", medico.nombre())
                .header("X-Rol", medico.rol().name());
    }

    //Envia la peticion y recibe la respuesta
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
                    "Peticion interrumpida", e);
        }
    }

//Comprueba el codigo HTTP recibido y lanza excepciones si no es el esperado
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
                "Los datos de la solicitud no son validos. "
                + "Comprueba los campos obligatorios.");

        case 403:
            throw new SecurityException(
                "No tienes permiso para realizar esta operacion.");

        case 404:
            throw new NoSuchElementException(
                "No se ha encontrado el paciente, "
                + "el tipo de prueba o la solicitud.");

        case 409:
            throw new IllegalStateException(
                "No se puede registrar la solicitud "
                + "porque existe un conflicto de datos.");

        default:
            throw new ServicioNoDisponibleException(
                "No se pudo completar la operacion. "
                + "Error HTTP: " + codigo);
    }
}

    //Consulta los tipos de prueba activos
    @Override
    public List<TipoPrueba> consultarTiposPrueba(
            Usuario medico) {

        HttpRequest peticion =
                prepararPeticion(
                        medico, "/solicitudes/tipos")
                .GET()
                .build();

        HttpResponse<String> respuesta = enviar(peticion);

        comprobarRespuesta(respuesta, 200);

        try {
            return GSON.fromJson(
                    respuesta.body(),
                    new TypeToken<List<TipoPrueba>>() {}
                            .getType());

        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "Respuesta de tipos de prueba no valida", e);
        }
    }

    // Registra una nueva solicitud
    @Override
    public SolicitudPrueba crearSolicitud(
            Usuario medico, SolicitudPrueba datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Faltan los datos de la solicitud");
        }

        String json = GSON.toJson(datos);

        HttpRequest peticion =
                prepararPeticion(
                        medico, "/solicitudes")
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> respuesta = enviar(peticion);

        comprobarRespuesta(respuesta, 201);

        try {
            return GSON.fromJson(
                    respuesta.body(), SolicitudPrueba.class);

        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "Respuesta de solicitud no valida", e);
        }
    }

    //Consulta una solicitud ya registrada
    @Override
    public SolicitudPrueba consultarSolicitud(
            Usuario medico, long id) {

        HttpRequest peticion =
                prepararPeticion(
                        medico, "/solicitudes/" + id)
                .GET()
                .build();

        HttpResponse<String> respuesta = enviar(peticion);

        comprobarRespuesta(respuesta, 200);

        try {
            return GSON.fromJson(
                    respuesta.body(), SolicitudPrueba.class);

        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "Respuesta de consulta no valida", e);
        }
    }
}
