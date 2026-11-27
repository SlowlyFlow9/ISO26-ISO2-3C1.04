package imagen;

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

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

public class ImagenesServiceHttp
        implements ImagenesService, ImagenesCreacionService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>)
                    (json, tipo, contexto) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private final HttpClient http;
    private final String urlBase;

    public ImagenesServiceHttp(String urlBase) {

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
            Usuario usuario,
            String ruta) throws SinPermisoException {

        if (usuario == null
                || usuario.nombre() == null
                || usuario.nombre().isBlank()
                || usuario.rol() != Rol.MEDICO
                || (usuario.medicoId() != null
                    && usuario.medicoId() <= 0)) {

            throw new SinPermisoException(
                    "El usuario no es un médico válido");
        }

        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(
                        URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Usuario", usuario.nombre())
                .header("X-Rol", usuario.rol().name());

        if (usuario.medicoId() != null) {

            peticion.header(
                    "X-Medico-Id",
                    usuario.medicoId().toString());
        }

        return peticion;
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

    private String campoError(
            HttpResponse<String> respuesta,
            String campo,
            String valorPorDefecto) {

        try {

            var json = JsonParser.parseString(
                    respuesta.body()).getAsJsonObject();

            if (json.has(campo)
                    && !json.get(campo).isJsonNull()) {

                return json.get(campo).getAsString();
            }

        } catch (RuntimeException e) {
            // La respuesta puede no contener JSON válido.
        }

        return valorPorDefecto;
    }

    private String mensajeError(
            HttpResponse<String> respuesta) {

        return campoError(
                respuesta,
                "mensaje",
                "Error HTTP " + respuesta.statusCode());
    }

    private void comprobarRespuesta(
            HttpResponse<String> respuesta)
            throws SinPermisoException {

        if (respuesta.statusCode() == 403) {

            throw new SinPermisoException(
                    mensajeError(respuesta));
        }

        if (respuesta.statusCode() == 400) {

            throw new IllegalArgumentException(
                    mensajeError(respuesta));
        }

        if (respuesta.statusCode() != 200) {

            throw new ServicioNoDisponibleException(
                    mensajeError(respuesta));
        }
    }

    private static void validarId(long id) {

        if (id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador debe ser positivo");
        }
    }

    // CU17 - Consultar listado de imágenes.
    @Override
    public List<ResumenImagenPrueba> listar(
            Usuario solicitante,
            long solicitudId)
            throws SinPermisoException,
            SolicitudImagenNoEncontradaException {

        validarId(solicitudId);

        HttpResponse<String> respuesta = enviar(
                prepararPeticion(
                        solicitante,
                        "/imagenes?solicitudId=" + solicitudId)
                        .GET()
                        .build());

        if (respuesta.statusCode() == 404) {

            throw new SolicitudImagenNoEncontradaException(
                    mensajeError(respuesta));
        }

        comprobarRespuesta(respuesta);

        try {

            List<ResumenImagenPrueba> imagenes = GSON.fromJson(
                    respuesta.body(),
                    new TypeToken<List<ResumenImagenPrueba>>() {}
                            .getType());

            if (imagenes == null) {

                throw new JsonParseException(
                        "Listado de imágenes no válido");
            }

            for (ResumenImagenPrueba imagen : imagenes) {

                if (imagen == null
                        || imagen.id() <= 0
                        || imagen.solicitudId() != solicitudId
                        || imagen.nombre() == null
                        || imagen.nombre().isBlank()
                        || imagen.tipoContenido() == null
                        || imagen.fechaCreacion() == null) {

                    throw new JsonParseException(
                            "Datos de imagen incorrectos");
                }
            }

            return List.copyOf(imagenes);

        } catch (RuntimeException e) {

            throw new ServicioNoDisponibleException(
                    "El listado de imágenes no es válido", e);
        }
    }

    // CU17 - Consultar una imagen.
    @Override
    public ImagenPrueba consultar(
            Usuario solicitante,
            long imagenId)
            throws SinPermisoException,
            ImagenNoEncontradaException,
            SolicitudImagenNoEncontradaException {

        validarId(imagenId);

        HttpResponse<String> respuesta = enviar(
                prepararPeticion(
                        solicitante,
                        "/imagenes/" + imagenId)
                        .GET()
                        .build());

        if (respuesta.statusCode() == 404) {

            String codigo = campoError(
                    respuesta, "error", "");

            if (codigo.equals("SOLICITUD_NO_ENCONTRADA")) {

                throw new SolicitudImagenNoEncontradaException(
                        mensajeError(respuesta));
            }

            throw new ImagenNoEncontradaException(
                    mensajeError(respuesta));
        }

        comprobarRespuesta(respuesta);

        try {

            ImagenPrueba imagen = GSON.fromJson(
                    respuesta.body(),
                    ImagenPrueba.class);

            if (imagen == null
                    || imagen.id() != imagenId
                    || imagen.solicitudId() <= 0
                    || imagen.nombre() == null
                    || imagen.nombre().isBlank()
                    || imagen.tipoContenido() == null
                    || imagen.contenido() == null
                    || imagen.contenido().isBlank()
                    || imagen.fechaCreacion() == null) {

                throw new JsonParseException(
                        "Datos de imagen incorrectos");
            }

            return imagen;

        } catch (RuntimeException e) {

            throw new ServicioNoDisponibleException(
                    "La respuesta de imagen no es válida", e);
        }
    }

    // CU18 - Registrar una imagen.
    @Override
    public ResumenImagenPrueba crear(
            Usuario solicitante,
            DatosCreacionImagen datos)
            throws SinPermisoException,
            SolicitudImagenNoEncontradaException,
            DatosImagenInvalidosException {

        if (solicitante == null
                || solicitante.medicoId() == null
                || solicitante.medicoId() <= 0) {

            throw new SinPermisoException(
                    "El médico debe estar identificado");
        }

        HttpRequest.Builder peticion =
                prepararPeticion(solicitante, "/imagenes");

        if (datos == null || datos.solicitudId() <= 0) {

            throw new DatosImagenInvalidosException(
                    "Los datos de la imagen no son válidos");
        }

        HttpResponse<String> respuesta = enviar(
                peticion
                        .timeout(Duration.ofSeconds(30))
                        .header(
                                "Content-Type",
                                "application/json; charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                GSON.toJson(datos)))
                        .build());

        int estado = respuesta.statusCode();

        if (estado == 403) {

            throw new SinPermisoException(
                    mensajeError(respuesta));
        }

        if (estado == 404) {

            throw new SolicitudImagenNoEncontradaException(
                    mensajeError(respuesta));
        }

        if (estado == 400 || estado == 413 || estado == 415) {

            throw new DatosImagenInvalidosException(
                    mensajeError(respuesta));
        }

        if (estado != 201) {

            throw new ServicioNoDisponibleException(
                    mensajeError(respuesta));
        }

        try {

            ResumenImagenPrueba imagen = GSON.fromJson(
                    respuesta.body(),
                    ResumenImagenPrueba.class);

            if (imagen == null
                    || imagen.id() <= 0
                    || imagen.solicitudId() != datos.solicitudId()
                    || imagen.medicoId() != solicitante.medicoId()
                    || imagen.nombre() == null
                    || imagen.nombre().isBlank()
                    || imagen.tipoContenido() == null
                    || imagen.fechaCreacion() == null) {

                throw new JsonParseException(
                        "Datos de imagen incorrectos");
            }

            return imagen;

        } catch (RuntimeException e) {

            throw new ServicioNoDisponibleException(
                    "La respuesta de creación no es válida", e);
        }
    }
}