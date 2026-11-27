package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorioMemoria;

class ImagenesServidorHttpTest {

    private HttpServer servidor;

    private final HttpClient cliente =
            HttpClient.newHttpClient();

    @BeforeEach
    void preparar() throws Exception {

        var solicitudes = new SolicitudesRepositorioMemoria();

        solicitudes.guardar(solicitud(1));
        solicitudes.guardar(solicitud(2));

        var imagenes = new ImagenesRepositorioMemoria();

        imagenes.guardar(new ImagenPrueba(
                0,
                1L,
                "radiografia.png",
                "image/png",
                "aW1hZ2Vu",
                1L,
                LocalDateTime.of(2026, 11, 27, 10, 0)));

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

    private SolicitudPrueba solicitud(long id) {

        return new SolicitudPrueba(
                id,
                10L,
                1L,
                20L,
                "Indicación clínica",
                "Justificación",
                Prioridad.ORDINARIA,
                LocalDateTime.of(2026, 11, 27, 9, 0),
                EstadoSolicitud.REALIZADA);
    }

    private HttpResponse<String> pedir(
            String ruta,
            String metodo,
            String rol) throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder(
                URI.create(
                        "http://127.0.0.1:"
                                + servidor.getAddress().getPort()
                                + ruta))
                .header("X-Usuario", "medico")
                .header("X-Medico-Id", "1");

        if (rol != null) {
            peticion.header("X-Rol", rol);
        }

        peticion.method(
                metodo,
                HttpRequest.BodyPublishers.noBody());

        return cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("GET de imágenes devuelve los datos sin su contenido")
    void listarImagenes() throws Exception {

        var respuesta = pedir(
                "/imagenes?solicitudId=1", "GET", "MEDICO");

        assertEquals(200, respuesta.statusCode());

        var listado = JsonParser.parseString(respuesta.body())
                .getAsJsonArray();

        assertEquals(1, listado.size());

        var imagen = listado.get(0).getAsJsonObject();

        assertAll(
                () -> assertEquals(
                        1L, imagen.get("id").getAsLong()),
                () -> assertEquals(
                        "radiografia.png",
                        imagen.get("nombre").getAsString()),
                () -> assertFalse(imagen.has("contenido"))
        );
    }

    @Test
    @DisplayName("GET de una imagen devuelve su contenido")
    void consultarImagen() throws Exception {

        var respuesta = pedir("/imagenes/1", "GET", "MEDICO");

        assertEquals(200, respuesta.statusCode());

        var imagen = JsonParser.parseString(respuesta.body())
                .getAsJsonObject();

        assertEquals(
                "aW1hZ2Vu",
                imagen.get("contenido").getAsString());
    }

    @Test
    @DisplayName("Una solicitud sin imágenes devuelve un array vacío")
    void listadoVacio() throws Exception {

        var respuesta = pedir(
                "/imagenes?solicitudId=2", "GET", "MEDICO");

        assertEquals(200, respuesta.statusCode());

        assertTrue(JsonParser.parseString(respuesta.body())
                .getAsJsonArray().isEmpty());
    }

    @Test
    @DisplayName("Una solicitud inexistente devuelve HTTP 404")
    void solicitudInexistente() throws Exception {

        var respuesta = pedir(
                "/imagenes?solicitudId=999", "GET", "MEDICO");

        assertEquals(404, respuesta.statusCode());

        assertTrue(respuesta.body()
                .contains("SOLICITUD_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("Una imagen inexistente devuelve HTTP 404")
    void imagenInexistente() throws Exception {

        var respuesta = pedir("/imagenes/999", "GET", "MEDICO");

        assertEquals(404, respuesta.statusCode());

        assertTrue(respuesta.body()
                .contains("IMAGEN_NO_ENCONTRADA"));
    }

    @ParameterizedTest(name = "Petición incorrecta: {0}")
    @ValueSource(strings = {
            "/imagenes",
            "/imagenes?solicitudId=abc",
            "/imagenes?solicitudId=0",
            "/imagenes?solicitudId=-1",
            "/imagenes/abc",
            "/imagenes/0",
            "/imagenes/-1",
            "/imagenes/999999999999999999999999"
    })
    void peticionInvalida(String ruta) throws Exception {

        assertEquals(
                400,
                pedir(ruta, "GET", "MEDICO").statusCode());
    }

    @ParameterizedTest(name = "Rol sin permiso: {0}")
    @ValueSource(strings = {
            "PACIENTE",
            "ADMINISTRADOR",
            "PERSONAL_ADMINISTRATIVO",
            "DESCONOCIDO"
    })
    void sinPermiso(String rol) throws Exception {

        var respuesta = pedir(
                "/imagenes?solicitudId=1", "GET", rol);

        assertEquals(403, respuesta.statusCode());

        assertTrue(respuesta.body().contains("SIN_PERMISO"));
    }

    @Test
    @DisplayName("Una petición sin rol devuelve HTTP 403")
    void sinRol() throws Exception {

        assertEquals(
                403,
                pedir("/imagenes/1", "GET", null).statusCode());
    }

    @Test
    @DisplayName("POST no está permitido todavía en CU17")
    void metodoNoPermitido() throws Exception {

        assertEquals(
                405,
                pedir("/imagenes", "POST", "MEDICO").statusCode());
    }

    @Test
    @DisplayName("Una ruta desconocida devuelve HTTP 404")
    void rutaInexistente() throws Exception {

        assertEquals(
                404,
                pedir("/imagenes/1/otra", "GET", "MEDICO")
                        .statusCode());
    }
}