package agenda;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
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

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

/** Cliente HTTP de CU14 para las agendas del médico solicitante. */
public class AgendasServiceHttp
        implements AgendasRealizacionService {

    private static class AdaptadorFecha
            implements JsonSerializer<LocalDate>,
                       JsonDeserializer<LocalDate> {

        @Override
        public JsonElement serialize(
                LocalDate fecha,
                Type tipo,
                JsonSerializationContext contexto) {

            return new JsonPrimitive(fecha.toString());
        }

        @Override
        public LocalDate deserialize(
                JsonElement json,
                Type tipo,
                JsonDeserializationContext contexto)
                throws JsonParseException {

            try {
                return LocalDate.parse(json.getAsString());
            } catch (RuntimeException e) {
                throw new JsonParseException(
                        "Fecha no válida en la respuesta", e);
            }
        }
    }

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
                    LocalDate.class,
                    new AdaptadorFecha())
            .registerTypeAdapter(
                    LocalTime.class,
                    new AdaptadorHora())
            .create();

    private final HttpClient http;
    private final String urlBase;

    public AgendasServiceHttp(String urlBase) {
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

    private HttpRequest.Builder prepararPeticion(
            Usuario solicitante,
            String ruta) {

        if (solicitante == null
                || solicitante.nombre() == null
                || solicitante.nombre().isBlank()
                || solicitante.rol() != Rol.MEDICO
                || solicitante.medicoId() == null
                || solicitante.medicoId() <= 0) {
            throw new IllegalArgumentException(
                    "Se requiere un médico con identificador válido");
        }

        return HttpRequest.newBuilder(
                    URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", solicitante.nombre())
                .header("X-Rol", solicitante.rol().name())
                .header("X-Medico-Id",
                        solicitante.medicoId().toString());
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
                    "La petición fue interrumpida", e);
        }
    }

    private void comprobarRespuesta(
            HttpResponse<String> respuesta,
            int codigoEsperado) {

        int codigo = respuesta.statusCode();
        if (codigo == codigoEsperado) {
            return;
        }

        switch (codigo) {
            case 400 -> throw new IllegalArgumentException(
                    "Los datos de la agenda no son válidos.");
            case 403 -> throw new SecurityException(
                    "No tienes permiso para gestionar esta agenda.");
            case 404 -> throw new NoSuchElementException(
                    "No se ha encontrado la agenda solicitada.");
            default -> throw new ServicioNoDisponibleException(
                    "Error del servidor. Código HTTP: " + codigo);
        }
    }

    private AgendaMedica leerAgenda(String cuerpo) {
        try {
            AgendaMedica agenda = GSON.fromJson(
                    cuerpo, AgendaMedica.class);

            if (agenda == null
                    || agenda.getId() == null
                    || agenda.getMedicoId() <= 0
                    || agenda.getFecha() == null
                    || agenda.getHoraInicio() == null
                    || agenda.getHoraFin() == null) {
                throw new IllegalStateException(
                        "Faltan datos de la agenda");
            }

            return agenda;
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "La respuesta de agenda no es válida", e);
        }
    }

    @Override
    public AgendaMedica crearPropia(
            Usuario solicitante,
            AgendaMedica datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos de la agenda son obligatorios");
        }

        HttpRequest peticion = prepararPeticion(
                        solicitante, "/agendas-realizacion")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        GSON.toJson(datos)))
                .build();

        HttpResponse<String> respuesta = enviar(peticion);
        comprobarRespuesta(respuesta, 201);
        return leerAgenda(respuesta.body());
    }

    @Override
    public AgendaMedica consultarPropia(
            Usuario solicitante,
            long id) {

        validarId(id);
        HttpRequest peticion = prepararPeticion(
                        solicitante, "/agendas-realizacion/" + id)
                .GET()
                .build();

        HttpResponse<String> respuesta = enviar(peticion);
        comprobarRespuesta(respuesta, 200);
        return leerAgenda(respuesta.body());
    }

    @Override
    public List<AgendaMedica> listarPropias(
            Usuario solicitante) {

        HttpRequest peticion = prepararPeticion(
                        solicitante, "/agendas-realizacion")
                .GET()
                .build();

        HttpResponse<String> respuesta = enviar(peticion);
        comprobarRespuesta(respuesta, 200);

        try {
            List<AgendaMedica> resultado = GSON.fromJson(
                    respuesta.body(),
                    new TypeToken<List<AgendaMedica>>() { }
                            .getType());
            if (resultado == null) {
                throw new IllegalStateException(
                        "El listado recibido es nulo");
            }
            return resultado;
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "El listado de agendas no es válido", e);
        }
    }

    @Override
    public AgendaMedica modificarPropia(
            Usuario solicitante,
            long id,
            AgendaMedica datos) {

        validarId(id);
        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos de la agenda son obligatorios");
        }

        HttpRequest peticion = prepararPeticion(
                        solicitante, "/agendas-realizacion/" + id)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        GSON.toJson(datos)))
                .build();

        HttpResponse<String> respuesta = enviar(peticion);
        comprobarRespuesta(respuesta, 200);
        return leerAgenda(respuesta.body());
    }

    @Override
    public void eliminarPropia(
            Usuario solicitante,
            long id) {

        validarId(id);
        HttpRequest peticion = prepararPeticion(
                        solicitante, "/agendas-realizacion/" + id)
                .DELETE()
                .build();

        comprobarRespuesta(enviar(peticion), 204);
    }

    private static void validarId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de agenda debe ser positivo");
        }
    }
}
