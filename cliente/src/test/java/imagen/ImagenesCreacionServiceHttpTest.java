package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

class ImagenesCreacionServiceHttpTest {

    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, 1L);

    private static final String RESPUESTA = """
            {
                "id":1,
                "solicitudId":1,
                "nombre":"radiografia.png",
                "tipoContenido":"image/png",
                "medicoId":1,
                "fechaCreacion":"2026-11-28T10:00"
            }
            """;

    private HttpServer servidor;
    private ImagenesServiceHttp servicio;

    private volatile int estado;
    private volatile String cuerpo;

    private volatile String peticionRecibida;
    private volatile String metodoRecibido;
    private volatile String rutaRecibida;
    private volatile String tipoRecibido;
    private volatile String medicoRecibido;
    private volatile String usuarioRecibido;
    private volatile String rolRecibido;

    private String contenido;

    @BeforeEach
    void preparar() throws Exception {

        estado = 201;
        cuerpo = RESPUESTA;

        contenido =
                VisorImagenTest.crearImagenPrueba();

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        servidor.createContext("/imagenes", exchange -> {

            metodoRecibido =
                    exchange.getRequestMethod();

            rutaRecibida =
                    exchange.getRequestURI().toString();

            tipoRecibido =
                    exchange.getRequestHeaders()
                            .getFirst("Content-Type");

            medicoRecibido =
                    exchange.getRequestHeaders()
                            .getFirst("X-Medico-Id");

            usuarioRecibido =
                    exchange.getRequestHeaders()
                            .getFirst("X-Usuario");

            rolRecibido =
                    exchange.getRequestHeaders()
                            .getFirst("X-Rol");

            peticionRecibida = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            byte[] bytes =
                    cuerpo.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json");

            exchange.sendResponseHeaders(
                    estado, bytes.length);

            try (var salida = exchange.getResponseBody()) {

                salida.write(bytes);
            }
        });

        servidor.start();

        servicio = new ImagenesServiceHttp(
                "http://127.0.0.1:"
                + servidor.getAddress().getPort());
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

    @Test
    @DisplayName("El cliente registra una imagen mediante POST")
    void crearImagen() throws Exception {

        ResumenImagenPrueba creada =
                servicio.crear(MEDICO, datosValidos());

        var json = JsonParser.parseString(peticionRecibida)
                .getAsJsonObject();

        assertAll(
                () -> assertEquals(1L, creada.id()),
                () -> assertEquals(1L, creada.solicitudId()),
                () -> assertEquals(1L, creada.medicoId()),
                () -> assertEquals(
                        "radiografia.png", creada.nombre()),
                () -> assertEquals("POST", metodoRecibido),
                () -> assertEquals("/imagenes", rutaRecibida),
                () -> assertEquals("MEDICO", rolRecibido),
                () -> assertEquals("medico", usuarioRecibido),
                () -> assertEquals("1", medicoRecibido),
                () -> assertTrue(
                        tipoRecibido.startsWith("application/json")),
                () -> assertEquals(
                        contenido,
                        json.get("contenido").getAsString()),
                () -> assertEquals(
                        1L, json.get("solicitudId").getAsLong()),
                () -> assertEquals(
                        "radiografia.png",
                        json.get("nombre").getAsString()),
                () -> assertEquals(
                        "image/png",
                        json.get("tipoContenido").getAsString()),
                () -> assertFalse(json.has("id")),
                () -> assertFalse(json.has("medicoId")),
                () -> assertFalse(json.has("fechaCreacion"))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 413, 415})
    void datosInvalidos(int codigo) {

        estado = codigo;
        cuerpo = """
                {"mensaje":"Imagen incorrecta"}
                """;

        var error = assertThrows(
                DatosImagenInvalidosException.class,
                () -> servicio.crear(MEDICO, datosValidos()));

        assertEquals(
                "Imagen incorrecta", error.getMessage());
    }

    @Test
    @DisplayName("HTTP 403 indica falta de permisos")
    void permisoRechazado() {

        estado = 403;
        cuerpo = """
                {"mensaje":"Acceso denegado"}
                """;

        var error = assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(MEDICO, datosValidos()));

        assertEquals(
                "Acceso denegado", error.getMessage());
    }

    @Test
    @DisplayName("HTTP 404 indica que la solicitud no existe")
    void solicitudInexistente() {

        estado = 404;
        cuerpo = """
                {"mensaje":"Solicitud inexistente"}
                """;

        assertThrows(
                SolicitudImagenNoEncontradaException.class,
                () -> servicio.crear(MEDICO, datosValidos()));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 405, 500, 503})
    void respuestaInesperada(int codigo) {

        estado = codigo;
        cuerpo = "Respuesta inesperada";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.crear(MEDICO, datosValidos()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null",
            "{}",
            "texto incorrecto"
    })
    void jsonIncorrecto(String respuesta) {

        cuerpo = respuesta;

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.crear(MEDICO, datosValidos()));
    }

    @Test
    @DisplayName("La respuesta debe corresponder a la solicitud enviada")
    void solicitudIncorrecta() {

        cuerpo = RESPUESTA.replace(
                "\"solicitudId\":1",
                "\"solicitudId\":2");

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.crear(MEDICO, datosValidos()));
    }

    @Test
    @DisplayName("La respuesta debe identificar al médico solicitante")
    void medicoIncorrecto() {

        cuerpo = RESPUESTA.replace(
                "\"medicoId\":1",
                "\"medicoId\":2");

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.crear(MEDICO, datosValidos()));
    }

    @ParameterizedTest
    @EnumSource(
            value = Rol.class,
            names = {"MEDICO"},
            mode = EnumSource.Mode.EXCLUDE)
    void usuarioSinPermiso(Rol rol) {

        Usuario usuario =
                new Usuario("usuario", rol, 1L);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(usuario, datosValidos()));

        assertNull(peticionRecibida);
    }

    @Test
    @DisplayName("Un usuario nulo no puede registrar imágenes")
    void usuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(null, datosValidos()));

        assertNull(peticionRecibida);
    }

    @Test
    @DisplayName("Un médico sin ID no realiza la petición")
    void medicoSinIdentificar() {

        Usuario usuario =
                new Usuario("medico", Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(usuario, datosValidos()));

        assertNull(peticionRecibida);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void medicoIdInvalido(long id) {

        Usuario usuario =
                new Usuario("medico", Rol.MEDICO, id);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(usuario, datosValidos()));

        assertNull(peticionRecibida);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void solicitudIdInvalido(long id) {

        var datos = new DatosCreacionImagen(
                id,
                "imagen.png",
                "image/png",
                contenido);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> servicio.crear(MEDICO, datos));

        assertNull(peticionRecibida);
    }

    @Test
    @DisplayName("Los datos nulos no se envían al servidor")
    void datosNulos() {

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> servicio.crear(MEDICO, null));

        assertNull(peticionRecibida);
    }

    @Test
    @DisplayName("Se detecta que el servidor está desconectado")
    void servidorDesconectado() {

        servidor.stop(0);
        servidor = null;

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.crear(MEDICO, datosValidos()));
    }
}