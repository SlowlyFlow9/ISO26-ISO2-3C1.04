package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

class SolicitudesServiceHttpCoberturaTest {

    private HttpServer servidor;
    private SolicitudesServiceHttp servicio;
    private Usuario medico;

    private int codigo;
    private String respuesta;
    private String ruta;

    @BeforeEach
    void setUp() throws IOException {

        codigo = 200;
        respuesta = "[]";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        servidor.createContext("/", exchange -> {

            ruta = exchange.getRequestURI().getPath();

            exchange.getRequestBody().readAllBytes();

            byte[] bytes =
                    respuesta.getBytes(
                            StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(
                    codigo,
                    bytes.length);

            exchange.getResponseBody()
                    .write(bytes);

            exchange.close();
        });

        servidor.start();

        String url =
                "http://127.0.0.1:"
                + servidor.getAddress().getPort()
                + "/";

        servicio =
                new SolicitudesServiceHttp(url);

        medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);
    }

    @AfterEach
    void tearDown() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private SolicitudPrueba datos() {

        return new SolicitudPrueba(
                0,
                1,
                2,
                3,
                "Dolor",
                "Control",
                Prioridad.ORDINARIA,
                null,
                null);
    }

    @Test
    @DisplayName(
            "La URL del servidor es obligatoria")
    void urlNulaNoPermitida() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SolicitudesServiceHttp(null));
    }

    @Test
    @DisplayName(
            "Una URL vacía no está permitida")
    void urlVaciaNoPermitida() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SolicitudesServiceHttp("   "));
    }

    @Test
    @DisplayName(
            "La barra final de la URL no duplica la ruta")
    void eliminaBarraFinal()
            throws Exception {

        servicio.consultarTiposPrueba(medico);

        assertEquals(
                "/solicitudes/tipos",
                ruta);
    }

    @Test
    @DisplayName(
            "Un usuario nulo no es válido")
    void usuarioNulo() {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultarTiposPrueba(
                        null));
    }

    @Test
    @DisplayName(
            "Un usuario sin rol no es válido")
    void usuarioSinRol() {

        Usuario usuario =
                new Usuario(
                        "medico",
                        null);

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultarTiposPrueba(
                        usuario));
    }

    @Test
    @DisplayName(
            "Un usuario sin nombre no es válido")
    void usuarioSinNombre() {

        Usuario usuario =
                new Usuario(
                        null,
                        Rol.MEDICO);

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultarTiposPrueba(
                        usuario));
    }

    @Test
    @DisplayName(
            "Un usuario con nombre vacío no es válido")
    void usuarioNombreVacio() {

        Usuario usuario =
                new Usuario(
                        "   ",
                        Rol.MEDICO);

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultarTiposPrueba(
                        usuario));
    }

    @Test
    @DisplayName(
            "Crear una solicitud requiere datos")
    void crearSinDatos() {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crearSolicitud(
                        medico,
                        null));
    }

    @Test
    @DisplayName(
            "Una respuesta 409 produce conflicto")
    void respuesta409() {

        codigo = 409;
        respuesta = "{}";

        assertThrows(
                IllegalStateException.class,
                () -> servicio.crearSolicitud(
                        medico,
                        datos()));
    }

    @Test
    @DisplayName(
            "JSON inválido al consultar tipos produce error de servicio")
    void jsonTiposInvalido() {

        codigo = 200;
        respuesta = "esto no es json";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.consultarTiposPrueba(
                        medico));
    }

    @Test
    @DisplayName(
            "JSON inválido al crear solicitud produce error de servicio")
    void jsonCrearInvalido() {

        codigo = 201;
        respuesta = "esto no es json";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.crearSolicitud(
                        medico,
                        datos()));
    }

    @Test
    @DisplayName(
            "JSON inválido al consultar solicitud produce error de servicio")
    void jsonConsultaInvalido() {

        codigo = 200;
        respuesta = "esto no es json";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.consultarSolicitud(
                        medico,
                        1L));
    }

    @Test
    @DisplayName(
            "Sin servidor se produce servicio no disponible")
    void servidorNoDisponible() {

        int puerto =
                servidor.getAddress().getPort();

        servidor.stop(0);
        servidor = null;

        SolicitudesServiceHttp sinServidor =
                new SolicitudesServiceHttp(
                        "http://127.0.0.1:"
                        + puerto);

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> sinServidor
                        .consultarTiposPrueba(medico));
    }
}