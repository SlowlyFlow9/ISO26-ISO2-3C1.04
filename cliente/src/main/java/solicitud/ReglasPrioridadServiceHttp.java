package solicitud;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

public class ReglasPrioridadServiceHttp
        implements ReglasPrioridadService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    new AdaptadorFechaHora())
            .create();

    private final HttpClient http;
    private final String urlBase;

    public ReglasPrioridadServiceHttp(String urlBase) {
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
    public ReglaPrioridad crear(
            Usuario solicitante,
            DatosReglaPrioridad datos)
            throws SinPermisoException {
        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos de la regla son obligatorios");
        }
        HttpRequest request = prepararPeticion(
                        solicitante, "/reglas-prioridad")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        GSON.toJson(datos)))
                .build();
        HttpResponse<String> respuesta = enviar(request);
        comprobarRespuesta(respuesta, 201);
        return leerRegla(respuesta.body());
    }

    @Override
    public List<ReglaPrioridad> listar(Usuario solicitante)
            throws SinPermisoException {
        HttpRequest request = prepararPeticion(
                        solicitante, "/reglas-prioridad")
                .GET()
                .build();
        HttpResponse<String> respuesta = enviar(request);
        comprobarRespuesta(respuesta, 200);

        try {
            List<ReglaPrioridad> reglas = GSON.fromJson(
                    respuesta.body(),
                    new TypeToken<List<ReglaPrioridad>>() { }.getType());
            if (reglas == null) {
                throw new IllegalStateException(
                        "El listado recibido es nulo");
            }
            List<ReglaPrioridad> resultado =
                    new ArrayList<>(reglas.size());
            for (ReglaPrioridad regla : reglas) {
                validarRegla(regla);
                resultado.add(regla);
            }
            return List.copyOf(resultado);
        } catch (JsonParseException | IllegalStateException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta del listado de reglas no es válida", e);
        }
    }

    @Override
    public ReglaPrioridad consultar(Usuario solicitante, long id)
            throws SinPermisoException {
        validarId(id);
        HttpRequest request = prepararPeticion(
                        solicitante, "/reglas-prioridad/" + id)
                .GET()
                .build();
        HttpResponse<String> respuesta = enviar(request);
        comprobarRespuesta(respuesta, 200);
        return leerRegla(respuesta.body());
    }

    @Override
    public ReglaPrioridad modificar(
            Usuario solicitante,
            long id,
            DatosReglaPrioridad datos)
            throws SinPermisoException {
        validarId(id);
        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos de la regla son obligatorios");
        }
        HttpRequest request = prepararPeticion(
                        solicitante, "/reglas-prioridad/" + id)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        GSON.toJson(datos)))
                .build();
        HttpResponse<String> respuesta = enviar(request);
        comprobarRespuesta(respuesta, 200);
        return leerRegla(respuesta.body());
    }

    @Override
    public void desactivar(Usuario solicitante, long id)
            throws SinPermisoException {
        validarId(id);
        HttpRequest request = prepararPeticion(
                        solicitante, "/reglas-prioridad/" + id)
                .DELETE()
                .build();
        comprobarRespuesta(enviar(request), 204);
    }

    private HttpRequest.Builder prepararPeticion(
            Usuario solicitante,
            String ruta) throws SinPermisoException {
        if (solicitante == null
                || solicitante.nombre() == null
                || solicitante.nombre().isBlank()
                || solicitante.rol() != Rol.ADMINISTRADOR) {
            throw new SinPermisoException(
                    "Se requiere un Administrador del Sistema");
        }
        return HttpRequest.newBuilder(URI.create(urlBase + ruta))
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
                    "La petición fue interrumpida", e);
        }
    }

    private static void comprobarRespuesta(
            HttpResponse<String> respuesta,
            int codigoEsperado) throws SinPermisoException {
        int codigo = respuesta.statusCode();
        if (codigo == codigoEsperado) {
            return;
        }

        String mensaje = mensajeError(respuesta);
        switch (codigo) {
            case 400, 409 -> throw new IllegalArgumentException(mensaje);
            case 403 -> throw new SinPermisoException(mensaje);
            case 404 -> throw new NoSuchElementException(mensaje);
            default -> throw new ServicioNoDisponibleException(
                    "Respuesta inesperada del servidor: " + codigo);
        }
    }

    private static ReglaPrioridad leerRegla(String cuerpo) {
        try {
            ReglaPrioridad regla = GSON.fromJson(
                    cuerpo, ReglaPrioridad.class);
            validarRegla(regla);
            return regla;
        } catch (JsonParseException | IllegalStateException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta de la regla no es válida", e);
        }
    }

    private static void validarRegla(ReglaPrioridad regla) {
        if (regla == null
                || regla.getId() <= 0
                || regla.getNombre() == null
                || regla.getNombre().isBlank()
                || regla.getDescripcion() == null
                || regla.getOrden() <= 0
                || regla.getModificadoPor() == null
                || regla.getModificadoPor().isBlank()
                || regla.getModificadoEn() == null) {
            throw new IllegalStateException(
                    "La regla recibida está incompleta");
        }
    }

    private static void validarId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la regla debe ser positivo");
        }
    }

    private static String mensajeError(
            HttpResponse<String> respuesta) {
        try {
            var json = JsonParser.parseString(
                    respuesta.body()).getAsJsonObject();
            if (json.has("mensaje")
                    && !json.get("mensaje").isJsonNull()) {
                return json.get("mensaje").getAsString();
            }
        } catch (JsonParseException | IllegalStateException e) {
            return "Error HTTP " + respuesta.statusCode();
        }
        return "Error HTTP " + respuesta.statusCode();
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
                        "Fecha y hora no válidas", e);
            }
        }
    }
}
