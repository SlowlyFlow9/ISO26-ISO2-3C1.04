package solicitud;

import java.io.IOException;
import java.lang.reflect.Type;
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
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

/**
 * CU28 - Cliente HTTP de la gestion de prioridades. Usa JSON (D-022) y las rutas de D-039:
 * GET /prioridades, GET /prioridades/reglas, PUT /prioridades/{id} y GET /prioridades/{id}/historial.
 */
public class PrioridadesServiceHttp implements PrioridadesService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>)
                    (fecha, tipo, contexto) -> new JsonPrimitive(fecha.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>)
                    (json, tipo, contexto) -> LocalDateTime.parse(json.getAsString()))
            .create();

    private static final String RUTA = "/prioridades";

    private final HttpClient http;
    private final String urlBase;

    public PrioridadesServiceHttp(String urlBase) {
        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalArgumentException("La dirección del servidor es obligatoria");
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
    public List<SolicitudPrioridad> consultarPendientes(Usuario solicitante, Prioridad prioridad) {
        String ruta = RUTA;
        if (prioridad != null) {
            ruta += "?prioridad=" + URLEncoder.encode(prioridad.name(), StandardCharsets.UTF_8);
        }
        HttpResponse<String> respuesta = enviar(peticion(solicitante, ruta).GET().build());
        comprobarRespuesta(respuesta);
        return leerLista(respuesta, new TypeToken<List<SolicitudPrioridad>>() { }.getType(),
                "La lista de solicitudes recibida no es válida");
    }

    @Override
    public List<ReglaPrioridadAplicable> consultarReglasActivas(Usuario solicitante) {
        HttpResponse<String> respuesta = enviar(peticion(solicitante, RUTA + "/reglas").GET().build());
        comprobarRespuesta(respuesta);
        return leerLista(respuesta, new TypeToken<List<ReglaPrioridadAplicable>>() { }.getType(),
                "Las reglas de prioridad recibidas no son válidas");
    }

    @Override
    public SolicitudPrioridad cambiarPrioridad(Usuario solicitante, long solicitudId,
            DatosCambioPrioridad datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Faltan los datos del cambio de prioridad");
        }
        HttpResponse<String> respuesta = enviar(peticion(solicitante, RUTA + "/" + solicitudId)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(GSON.toJson(datos)))
                .build());
        comprobarRespuesta(respuesta);
        try {
            SolicitudPrioridad actualizada = GSON.fromJson(respuesta.body(), SolicitudPrioridad.class);
            if (actualizada == null || actualizada.prioridad() == null) {
                throw new IllegalStateException("Respuesta incompleta");
            }
            return actualizada;
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException("La respuesta del cambio de prioridad no es válida", e);
        }
    }

    @Override
    public List<CambioPrioridad> consultarHistorial(Usuario solicitante, long solicitudId) {
        HttpResponse<String> respuesta = enviar(
                peticion(solicitante, RUTA + "/" + solicitudId + "/historial").GET().build());
        comprobarRespuesta(respuesta);
        return leerLista(respuesta, new TypeToken<List<CambioPrioridad>>() { }.getType(),
                "El historial de prioridades recibido no es válido");
    }

    // ---- Utilidades ----

    private HttpRequest.Builder peticion(Usuario solicitante, String ruta) {
        if (solicitante == null || solicitante.nombre() == null
                || solicitante.nombre().isBlank() || solicitante.rol() == null) {
            throw new IllegalArgumentException("El usuario no es válido");
        }
        return HttpRequest.newBuilder(URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", solicitante.nombre())
                .header("X-Rol", solicitante.rol().name());
    }

    private HttpResponse<String> enviar(HttpRequest peticion) {
        try {
            return http.send(peticion, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ServicioNoDisponibleException("No se pudo conectar con el servidor", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException("La petición fue interrumpida", e);
        }
    }

    private static <T> List<T> leerLista(HttpResponse<String> respuesta, Type tipo, String mensajeError) {
        try {
            List<T> lista = GSON.fromJson(respuesta.body(), tipo);
            if (lista == null || lista.contains(null)) {
                throw new IllegalStateException("Listado incompleto");
            }
            return List.copyOf(lista);
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(mensajeError, e);
        }
    }

    //Traduce los codigos de error de D-039 a excepciones con un mensaje para la pantalla
    private static void comprobarRespuesta(HttpResponse<String> respuesta) {
        int codigo = respuesta.statusCode();
        if (codigo == 200) {
            return;
        }
        switch (codigo) {
            case 400 -> throw new IllegalArgumentException(
                    "Los datos del cambio de prioridad no son válidos.");
            case 403 -> throw new SecurityException(
                    "No tienes permiso para gestionar prioridades.");
            case 404 -> throw new NoSuchElementException(
                    "No se ha encontrado la solicitud o la regla de prioridad.");
            case 409 -> throw new IllegalStateException(
                    "La solicitud ya no está pendiente de citación, "
                    + "por lo que no se puede cambiar su prioridad.");
            default -> throw new ServicioNoDisponibleException(
                    "No se pudo completar la operación. Error HTTP: " + codigo);
        }
    }
}
