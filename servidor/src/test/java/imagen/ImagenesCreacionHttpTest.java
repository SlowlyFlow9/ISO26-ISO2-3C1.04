package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorioMemoria;

class ImagenesCreacionHttpTest {

    private final Gson gson = new Gson();

    private final HttpClient cliente =
            HttpClient.newHttpClient();

    private HttpServer servidor;

    private ImagenesRepositorioMemoria imagenes;

    private String contenido;

    @BeforeEach
    void preparar() throws Exception {

        imagenes = new ImagenesRepositorioMemoria();

        var solicitudes =
                new SolicitudesRepositorioMemoria();

        solicitudes.guardar(new SolicitudPrueba(
                1,
                10L,
                1L,
                20L,
                "Indicación clínica",
                "Justificación",
                Prioridad.ORDINARIA,
                LocalDateTime.of(2026, 11, 27, 9, 0),
                EstadoSolicitud.REALIZADA));

        contenido =
                ValidadorImagenTest.crearImagen("png");

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        ImagenesServidorHttp.registrar(
                servidor,
                new ImagenesServiceImpl(imagenes, solicitudes));

        servidor.start();
    }

    @AfterEach
    void cerrar() {

        if (servidor != null) {

            servidor.stop(0);
        }
    }

    private DatosCreacionImagen datosValidos() {

        return new DatosCreacionImagen(
                1,
                "radiografia.png",
                "image/png",
                contenido);
    }

    private HttpResponse<String> pedir(
            String ruta,
            String metodo,
            String rol,
            String medicoId,
            String tipo,
            String cuerpo) throws Exception {

        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(
                        URI.create(
                                "http://127.0.0.1:"
                                + servidor.getAddress().getPort()
                                + ruta))
                        .timeout(Duration.ofSeconds(15))
                        .header("X-Usuario", "medico");

        if (rol != null) {

            peticion.header("X-Rol", rol);
        }

        if (medicoId != null) {

            peticion.header("X-Medico-Id", medicoId);
        }

        if (tipo != null) {

            peticion.header("Content-Type", tipo);
        }

        peticion.method(
                metodo,
                cuerpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo));

        return cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> crear(String cuerpo)
            throws Exception {

        return pedir(
                "/imagenes",
                "POST",
                "MEDICO",
                "1",
                "application/json",
                cuerpo);
    }

    @Test
    @DisplayName("POST registra la imagen y devuelve HTTP 201")
    void crearImagen() throws Exception {

        var respuesta = crear(
                gson.toJson(datosValidos()));

        assertEquals(201, respuesta.statusCode());

        var imagen = JsonParser.parseString(respuesta.body())
                .getAsJsonObject();

        assertAll(
                () -> assertEquals(
                        1L, imagen.get("id").getAsLong()),
                () -> assertEquals(
                        1L, imagen.get("solicitudId").getAsLong()),
                () -> assertEquals(
                        1L, imagen.get("medicoId").getAsLong()),
                () -> assertEquals(
                        "radiografia.png",
                        imagen.get("nombre").getAsString()),
                () -> assertFalse(imagen.has("contenido")),
                () -> assertEquals(
                        "/imagenes/1",
                        respuesta.headers()
                                .firstValue("Location")
                                .orElseThrow()),
                () -> assertNotNull(
                        LocalDateTime.parse(
                                imagen.get("fechaCreacion")
                                        .getAsString()))
        );
    }

    @Test
    @DisplayName("Una imagen creada por HTTP se puede consultar por HTTP")
    void crearYConsultar() throws Exception {

        var creada = crear(
                gson.toJson(datosValidos()));

        assertEquals(201, creada.statusCode());

        var detalle = pedir(
                "/imagenes/1",
                "GET",
                "MEDICO",
                "1",
                null,
                null);

        assertEquals(200, detalle.statusCode());

        var imagen = JsonParser.parseString(detalle.body())
                .getAsJsonObject();

        assertEquals(
                contenido,
                imagen.get("contenido").getAsString());

        var listado = pedir(
                "/imagenes?solicitudId=1",
                "GET",
                "MEDICO",
                "1",
                null,
                null);

        assertEquals(200, listado.statusCode());

        var resultado = JsonParser.parseString(listado.body())
                .getAsJsonArray();

        assertEquals(1, resultado.size());

        assertEquals(
                1L,
                resultado.get(0)
                        .getAsJsonObject()
                        .get("id")
                        .getAsLong());

        assertFalse(
                resultado.get(0)
                        .getAsJsonObject()
                        .has("contenido"));
    }

    @Test
    @DisplayName("Una solicitud inexistente devuelve HTTP 404")
    void solicitudInexistente() throws Exception {

        var datos = new DatosCreacionImagen(
                999,
                "radiografia.png",
                "image/png",
                contenido);

        var respuesta = crear(gson.toJson(datos));

        assertEquals(404, respuesta.statusCode());

        assertTrue(respuesta.body()
                .contains("SOLICITUD_NO_ENCONTRADA"));

        assertTrue(
                imagenes.listarPorSolicitud(999).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "null",
            "{}",
            "{",
            "texto incorrecto"
    })
    void jsonInvalido(String cuerpo) throws Exception {

        var respuesta = crear(cuerpo);

        assertEquals(400, respuesta.statusCode());

        assertTrue(respuesta.body()
                .contains("DATOS_INVALIDOS"));

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("Una imagen inválida devuelve HTTP 400")
    void imagenInvalida() throws Exception {

        var datos = new DatosCreacionImagen(
                1,
                "imagen.png",
                "image/png",
                "%%%");

        var respuesta = crear(gson.toJson(datos));

        assertEquals(400, respuesta.statusCode());

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PACIENTE",
            "ADMINISTRADOR",
            "PERSONAL_ADMINISTRATIVO",
            "DESCONOCIDO"
    })
    void usuarioSinPermiso(String rol) throws Exception {

        var respuesta = pedir(
                "/imagenes",
                "POST",
                rol,
                "1",
                "application/json",
                gson.toJson(datosValidos()));

        assertEquals(403, respuesta.statusCode());

        assertTrue(respuesta.body()
                .contains("SIN_PERMISO"));

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("Un médico sin ID recibe HTTP 403")
    void medicoSinIdentificar() throws Exception {

        var respuesta = pedir(
                "/imagenes",
                "POST",
                "MEDICO",
                null,
                "application/json",
                gson.toJson(datosValidos()));

        assertEquals(403, respuesta.statusCode());

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("Una petición sin rol recibe HTTP 403")
    void sinRol() throws Exception {

        var respuesta = pedir(
                "/imagenes",
                "POST",
                null,
                "1",
                "application/json",
                gson.toJson(datosValidos()));

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    @DisplayName("El servidor asigna el ID y el médico responsable")
    void datosAsignadosPorServidor() throws Exception {

        var datos = gson.toJsonTree(datosValidos())
                .getAsJsonObject();

        datos.addProperty("id", 999);
        datos.addProperty("medicoId", 999);
        datos.addProperty(
                "fechaCreacion", "2000-01-01T00:00:00");

        var respuesta = crear(datos.toString());

        assertEquals(201, respuesta.statusCode());

        var creada = JsonParser.parseString(respuesta.body())
                .getAsJsonObject();

        assertEquals(1L, creada.get("id").getAsLong());
        assertEquals(1L, creada.get("medicoId").getAsLong());

        assertNotEquals(
                "2000-01-01T00:00:00",
                creada.get("fechaCreacion").getAsString());
    }

    @Test
    @DisplayName("Un tipo de contenido no admitido devuelve HTTP 415")
    void tipoContenidoIncorrecto() throws Exception {

        var respuesta = pedir(
                "/imagenes",
                "POST",
                "MEDICO",
                "1",
                "text/plain",
                gson.toJson(datosValidos()));

        assertEquals(415, respuesta.statusCode());

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("Una petición demasiado grande devuelve HTTP 413")
    void peticionDemasiadoGrande() throws Exception {

        String cuerpo =
                "A".repeat(8 * 1024 * 1024 + 1);

        var respuesta = crear(cuerpo);

        assertEquals(413, respuesta.statusCode());

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("No se permite crear mediante la ruta de detalle")
    void rutaCreacionIncorrecta() throws Exception {

        var respuesta = pedir(
                "/imagenes/1",
                "POST",
                "MEDICO",
                "1",
                "application/json",
                gson.toJson(datosValidos()));

        assertEquals(405, respuesta.statusCode());

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }
}