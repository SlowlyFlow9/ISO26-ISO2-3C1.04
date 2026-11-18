package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class PacientesServiceHttpCoberturaTest {

    private HttpServer servidor;

    private int codigo;
    private String respuesta;
    private String ruta;

    private Usuario personal;

    @BeforeEach
    void setUp() throws IOException {

        codigo = 201;

        respuesta =
                "{"
                + "\"id\":1,"
                + "\"nombre\":\"Ana\","
                + "\"apellidos\":\"Pérez\","
                + "\"dni\":\"12345678Z\","
                + "\"fechaNacimiento\":\"1990-05-01\","
                + "\"telefono\":\"600111222\","
                + "\"activo\":true"
                + "}";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0);

        servidor.createContext("/", exchange -> {

            ruta =
                    exchange.getRequestURI()
                            .getPath();

            exchange.getRequestBody()
                    .readAllBytes();

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

        personal =
                new Usuario(
                        "personal",
                        Rol.PERSONAL_ADMINISTRATIVO);
    }

    @AfterEach
    void tearDown() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private PacientesServiceHttp servicio() {

        return new PacientesServiceHttp(
                "http://127.0.0.1:"
                + servidor.getAddress().getPort()
                + "/");
    }

    private Paciente paciente() {

        Paciente paciente = new Paciente();

        paciente.setNombre("Ana");
        paciente.setApellidos("Pérez");
        paciente.setDni("12345678Z");

        paciente.setFechaNacimiento(
                LocalDate.of(
                        1990,
                        5,
                        1));

        paciente.setTelefono(
                "600111222");

        return paciente;
    }

    @Test
    @DisplayName(
            "La barra final de la URL se elimina")
    void eliminaBarraFinal()
            throws Exception {

        servicio().crear(
                personal,
                paciente());

        assertEquals(
                "/pacientes",
                ruta);
    }

    @Test
    @DisplayName(
            "Buscar todavía no está conectado al servidor")
    void buscarNoDisponible() {

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio().buscar(
                        personal,
                        "Ana"));
    }

    @Test
    @DisplayName(
            "Consultar todavía no está conectado al servidor")
    void consultarNoDisponible() {

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio().consultar(
                        personal,
                        1L));
    }

    @Test
    @DisplayName(
            "Modificar todavía no está conectado al servidor")
    void modificarNoDisponible() {

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio().modificar(
                        personal,
                        1L,
                        paciente()));
    }

    @Test
    @DisplayName(
            "Una respuesta inesperada al eliminar produce error de servicio")
    void eliminarRespuestaInesperada() {

        codigo = 500;
        respuesta = "{}";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio().desactivar(
                        personal,
                        1L));
    }

    @Test
    @DisplayName(
            "Una respuesta no válida al eliminar produce error de servicio")
    void eliminarRespuestaNoValida() {

        codigo = 403;
        respuesta = "no es json";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio().desactivar(
                        personal,
                        1L));
    }

    @Test
    @DisplayName(
            "Una respuesta 400 con JSON inválido produce error de servicio")
    void crearRespuesta400NoValida() {

        codigo = 400;
        respuesta = "no es json";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio().crear(
                        personal,
                        paciente()));
    }

    @Test
    @DisplayName(
            "Sin servidor la baja produce servicio no disponible")
    void eliminarSinServidor() {

        int puerto =
                servidor.getAddress().getPort();

        servidor.stop(0);
        servidor = null;

        PacientesServiceHttp servicio =
                new PacientesServiceHttp(
                        "http://127.0.0.1:"
                        + puerto);

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.desactivar(
                        personal,
                        1L));
    }
}