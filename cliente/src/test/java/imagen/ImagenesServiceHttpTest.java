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

import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

class ImagenesServiceHttpTest {

    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, 1L);

    private static final String RESUMEN = """
            {
                "id":1,
                "solicitudId":1,
                "nombre":"radiografia.png",
                "tipoContenido":"image/png",
                "medicoId":1,
                "fechaCreacion":"2026-11-27T10:00"
            }
            """;

    private static final String IMAGEN = """
            {
                "id":1,
                "solicitudId":1,
                "nombre":"radiografia.png",
                "tipoContenido":"image/png",
                "contenido":"aW1hZ2Vu",
                "medicoId":1,
                "fechaCreacion":"2026-11-27T10:00"
            }
            """;

    private HttpServer servidor;
    private ImagenesServiceHttp servicio;

    private volatile int estado;
    private volatile String cuerpo;

    private volatile String rutaRecibida;
    private volatile String rolRecibido;
    private volatile String usuarioRecibido;
    private volatile String medicoRecibido;
    private volatile String metodoRecibido;

    @BeforeEach
    void preparar() throws Exception {

        estado = 200;
        cuerpo = "[" + RESUMEN + "]";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        servidor.createContext("/imagenes", exchange -> {

            rutaRecibida =
                    exchange.getRequestURI().toString();

            rolRecibido =
                    exchange.getRequestHeaders()
                            .getFirst("X-Rol");

            usuarioRecibido =
                    exchange.getRequestHeaders()
                            .getFirst("X-Usuario");

            medicoRecibido =
                    exchange.getRequestHeaders()
                            .getFirst("X-Medico-Id");

            metodoRecibido =
                    exchange.getRequestMethod();

            byte[] datos =
                    cuerpo.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json");

            exchange.sendResponseHeaders(
                    estado, datos.length);

            try (var salida = exchange.getResponseBody()) {

                salida.write(datos);
            }
        });

        servidor.start();

        servicio = new ImagenesServiceHttp(
                "http://127.0.0.1:"
                + servidor.getAddress().getPort() + "/");
    }

    @AfterEach
    void cerrar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("El cliente solicita las imágenes con las cabeceras correctas")
    void listarImagenes() throws Exception {

        var resultado = servicio.listar(MEDICO, 1);

        assertEquals(1, resultado.size());

        var imagen = resultado.get(0);

        assertAll(
                () -> assertEquals(1L, imagen.id()),
                () -> assertEquals(1L, imagen.solicitudId()),
                () -> assertEquals(
                        "radiografia.png", imagen.nombre()),
                () -> assertEquals(
                        "image/png", imagen.tipoContenido()),
                () -> assertEquals(
                        "/imagenes?solicitudId=1", rutaRecibida),
                () -> assertEquals("GET", metodoRecibido),
                () -> assertEquals("MEDICO", rolRecibido),
                () -> assertEquals("medico", usuarioRecibido),
                () -> assertEquals("1", medicoRecibido)
        );
    }

    @Test
    @DisplayName("Una solicitud sin imágenes devuelve una lista vacía")
    void listadoVacio() throws Exception {

        cuerpo = "[]";

        assertTrue(servicio.listar(MEDICO, 1).isEmpty());
    }

    @Test
    @DisplayName("El cliente recupera el contenido de la imagen")
    void consultarImagen() throws Exception {

        cuerpo = IMAGEN;

        ImagenPrueba resultado =
                servicio.consultar(MEDICO, 1);

        assertAll(
                () -> assertEquals(1L, resultado.id()),
                () -> assertEquals(
                        "aW1hZ2Vu", resultado.contenido()),
                () -> assertEquals(
                        "/imagenes/1", rutaRecibida)
        );
    }

    @Test
    @DisplayName("HTTP 404 al listar indica solicitud inexistente")
    void solicitudInexistente() {

        estado = 404;

        cuerpo = """
                {
                    "error":"SOLICITUD_NO_ENCONTRADA",
                    "mensaje":"Solicitud inexistente"
                }
                """;

        var error = assertThrows(
                SolicitudImagenNoEncontradaException.class,
                () -> servicio.listar(MEDICO, 999));

        assertEquals(
                "Solicitud inexistente",
                error.getMessage());
    }

    @Test
    @DisplayName("HTTP 404 al consultar indica imagen inexistente")
    void imagenInexistente() {

        estado = 404;

        cuerpo = """
                {
                    "error":"IMAGEN_NO_ENCONTRADA",
                    "mensaje":"Imagen inexistente"
                }
                """;

        assertThrows(
                ImagenNoEncontradaException.class,
                () -> servicio.consultar(MEDICO, 999));
    }

    @Test
    @DisplayName("Se distingue una imagen cuya solicitud no existe")
    void imagenSinSolicitud() {

        estado = 404;

        cuerpo = """
                {
                    "error":"SOLICITUD_NO_ENCONTRADA",
                    "mensaje":"Solicitud inexistente"
                }
                """;

        assertThrows(
                SolicitudImagenNoEncontradaException.class,
                () -> servicio.consultar(MEDICO, 1));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void identificadorInvalido(long id) {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.listar(MEDICO, id));

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultar(MEDICO, id));

        assertNull(rutaRecibida);
    }

    @ParameterizedTest
    @EnumSource(
            value = Rol.class,
            names = {"MEDICO"},
            mode = EnumSource.Mode.EXCLUDE)
    void usuarioSinPermiso(Rol rol) {

        Usuario usuario =
                new Usuario("usuario", rol);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.listar(usuario, 1));

        assertThrows(
                SinPermisoException.class,
                () -> servicio.consultar(usuario, 1));

        assertNull(rutaRecibida);
    }

    @Test
    @DisplayName("El usuario nulo no realiza peticiones")
    void usuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.listar(null, 1));

        assertNull(rutaRecibida);
    }

    @Test
    @DisplayName("HTTP 403 genera una excepción de permiso")
    void permisoRechazadoPorServidor() {

        estado = 403;
        cuerpo = """
                {"mensaje":"Acceso denegado"}
                """;

        var error = assertThrows(
                SinPermisoException.class,
                () -> servicio.listar(MEDICO, 1));

        assertEquals(
                "Acceso denegado",
                error.getMessage());
    }

    @Test
    @DisplayName("HTTP 400 indica datos incorrectos")
    void datosInvalidos() {

        estado = 400;
        cuerpo = """
                {"mensaje":"Datos incorrectos"}
                """;

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultar(MEDICO, 1));
    }

    @Test
    @DisplayName("HTTP 500 indica un problema del servidor")
    void errorServidor() {

        estado = 500;
        cuerpo = "Error interno";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listar(MEDICO, 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "texto incorrecto",
            "null",
            "[null]",
            "[{}]"
    })
    void listadoIncorrecto(String respuesta) {

        cuerpo = respuesta;

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listar(MEDICO, 1));
    }

    @Test
    @DisplayName("No se aceptan imágenes de otra solicitud")
    void solicitudDistinta() {

        cuerpo = "[" + RESUMEN.replace(
                "\"solicitudId\":1",
                "\"solicitudId\":2") + "]";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listar(MEDICO, 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "texto incorrecto",
            "null",
            "{}"
    })
    void imagenIncorrecta(String respuesta) {

        cuerpo = respuesta;

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.consultar(MEDICO, 1));
    }

    @Test
    @DisplayName("Se rechaza una imagen con un ID diferente al solicitado")
    void imagenDistinta() {

        cuerpo = IMAGEN.replace(
                "\"id\":1", "\"id\":2");

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.consultar(MEDICO, 1));
    }

    @Test
    @DisplayName("Se rechaza una imagen sin contenido")
    void imagenSinContenido() {

        cuerpo = IMAGEN.replace(
                "aW1hZ2Vu", "");

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.consultar(MEDICO, 1));
    }

    @Test
    @DisplayName("Se detecta que el servidor no está disponible")
    void servidorDesconectado() {

        servidor.stop(0);
        servidor = null;

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listar(MEDICO, 1));
    }

    @Test
    @DisplayName("La dirección del servidor es obligatoria")
    void direccionInvalida() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ImagenesServiceHttp(null));

        assertThrows(
                IllegalArgumentException.class,
                () -> new ImagenesServiceHttp(" "));
    }
}