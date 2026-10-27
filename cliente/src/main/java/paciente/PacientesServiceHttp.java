package paciente;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Cliente HTTP del servidor (D-022): POST /pacientes con el usuario en las cabeceras X-Usuario y X-Rol (D-023). */
public class PacientesServiceHttp implements PacientesService {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (f, t, c) -> new JsonPrimitive(f.toString()))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (j, t, c) -> LocalDate.parse(j.getAsString()))
            .create();

    private final HttpClient http = HttpClient.newHttpClient();
    private final String urlBase;

    public PacientesServiceHttp(String urlBase) {
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    @Override
    public Paciente crear(Usuario solicitante, Paciente datos)
            throws SinPermisoException, DatosInvalidosException, PacienteDuplicadoException {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlBase + "/pacientes"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Usuario", solicitante.nombre())
                .header("X-Rol", solicitante.rol().name())
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(datos)))
                .build();
        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ServicioNoDisponibleException("No se pudo conectar con el servidor", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException("Petición interrumpida", e);
        }
        try {
            switch (respuesta.statusCode()) {
                case 201: return GSON.fromJson(respuesta.body(), Paciente.class);
                case 400: throw new DatosInvalidosException(errores(respuesta.body()));
                case 403: throw new SinPermisoException(mensaje(respuesta.body()));
                case 409: throw new PacienteDuplicadoException(mensaje(respuesta.body()));
                default: throw new ServicioNoDisponibleException("Respuesta inesperada del servidor: " + respuesta.statusCode());
            }
        } catch (RuntimeException e) {
            if (e instanceof ServicioNoDisponibleException) throw e;
            throw new ServicioNoDisponibleException("Respuesta del servidor no válida", e);
        }
    }

    private static String mensaje(String cuerpo) {
        return JsonParser.parseString(cuerpo).getAsJsonObject().get("mensaje").getAsString();
    }

    private static List<String> errores(String cuerpo) {
        JsonArray lista = JsonParser.parseString(cuerpo).getAsJsonObject().getAsJsonArray("errores");
        List<String> errores = new ArrayList<>();
        for (JsonElement e : lista) errores.add(e.getAsString());
        return errores;
    }
}
