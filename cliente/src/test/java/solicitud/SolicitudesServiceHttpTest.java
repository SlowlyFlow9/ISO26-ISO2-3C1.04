package solicitud;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.Usuario;
import paciente.ServicioNoDisponibleException;
import tipoprueba.TipoPrueba;

/**
 * Pruebas del cliente HTTP de CU11.
 * Prescribir pruebas diagnosticas.
 */
public class SolicitudesServiceHttpTest {

    private HttpServer servidor;
    private SolicitudesServiceHttp servicio;
    private Usuario medico;

    private volatile int codigoRespuesta;
    private volatile String respuesta;
    private volatile String rutaRecibida;
    private volatile String metodoRecibido;
    private volatile String rolRecibido;
    private volatile String usuarioRecibido;
    private volatile String cuerpoRecibido;

    // Preparamos un servidor HTTP de prueba.
    @BeforeEach
    public void preparar() throws IOException {

        codigoRespuesta = 200;
        respuesta = "[]";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/", exchange -> {

            rutaRecibida =
                    exchange.getRequestURI().getPath();

            metodoRecibido =
                    exchange.getRequestMethod();

            rolRecibido =
                    exchange.getRequestHeaders()
                            .getFirst("X-Rol");

            usuarioRecibido =
                    exchange.getRequestHeaders()
                            .getFirst("X-Usuario");

            cuerpoRecibido = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            byte[] bytes = respuesta.getBytes(
                    StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json");

            exchange.sendResponseHeaders(
                    codigoRespuesta, bytes.length);

            try (var salida = exchange.getResponseBody()) {
                salida.write(bytes);
            }
        });

        servidor.start();

        String url = "http://127.0.0.1:"
                + servidor.getAddress().getPort();

        servicio = new SolicitudesServiceHttp(url);

        medico = new Usuario("Medico de prueba", Rol.MEDICO);
    }

    //Apagamos el servidor despues de cada prueba.
    @AfterEach
    public void terminar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    //Creamos una solicitud de ejemplo.
    private SolicitudPrueba crearDatos() {

        return new SolicitudPrueba(
                0,
                1,
                2,
                3,
                "Dolor de rodilla",
                "Radiografia necesaria",
                Prioridad.PREFERENTE,
                null,
                null
        );
    }

    //JSON que devolvería el servidor.
    private String solicitudRegistrada() {

        return "{"
                + "\"id\":5,"
                + "\"pacienteId\":1,"
                + "\"medicoId\":2,"
                + "\"tipoPruebaId\":3,"
                + "\"indicacionClinica\":\"Dolor de rodilla\","
                + "\"justificacion\":\"Radiografia necesaria\","
                + "\"prioridad\":\"PREFERENTE\","
                + "\"fechaSolicitud\":\"2026-11-09T10:30:00\","
                + "\"estado\":\"PENDIENTE_CITACION\""
                + "}";
    }

    //Prueba 1: consultar los tipos de prueba disponibles.
    @Test
    public void consultarTiposDisponibles() {

        respuesta = "[{"
                + "\"id\":3,"
                + "\"nombre\":\"Radiografia\","
                + "\"descripcion\":\"Prueba diagnostica\","
                + "\"duracionEstimada\":15,"
                + "\"requierePreparacion\":false,"
                + "\"instruccionesPaciente\":\"\","
                + "\"activo\":true"
                + "}]";

        List<TipoPrueba> tipos =
                servicio.consultarTiposPrueba(medico);

        assertEquals(1, tipos.size());

        assertEquals(
                "Radiografia",
                tipos.get(0).getNombre());

        assertEquals("/solicitudes/tipos", rutaRecibida);
        assertEquals("GET", metodoRecibido);
        assertEquals("MEDICO", rolRecibido);
    }

    //Prueba 2: registrar correctamente una solicitud
    @Test
    public void registrarSolicitudCorrectamente() {

        codigoRespuesta = 201;
        respuesta = solicitudRegistrada();

        SolicitudPrueba creada =
                servicio.crearSolicitud(
                        medico, crearDatos());

        assertEquals(5, creada.getId());

        assertEquals(
                EstadoSolicitud.PENDIENTE_CITACION,
                creada.getEstado());

        assertEquals(
                Prioridad.PREFERENTE,
                creada.getPrioridad());

        assertEquals(
                LocalDateTime.of(2026, 11, 9, 10, 30),
                creada.getFechaSolicitud());

        assertEquals("/solicitudes", rutaRecibida);
        assertEquals("POST", metodoRecibido);
        assertEquals("MEDICO", rolRecibido);

        assertEquals(
                "Medico de prueba", usuarioRecibido);

        assertTrue(
                cuerpoRecibido.contains(
                        "\"pacienteId\":1"));
    }

    //Prueba 3: consultar una solicitud por su identificador.
    @Test
    public void consultarSolicitudPorId() {

        respuesta = solicitudRegistrada();

        SolicitudPrueba solicitud =
                servicio.consultarSolicitud(medico, 5);

        assertEquals(5, solicitud.getId());
        assertEquals("/solicitudes/5", rutaRecibida);
        assertEquals("GET", metodoRecibido);
    }

    //Prueba 4: comprobar el mensaje de datos invalidos.
    @Test
    public void rechazarDatosInvalidos() {

        codigoRespuesta = 400;
        respuesta = "{\"error\":\"DATOS_INVALIDOS\"}";

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crearSolicitud(
                        medico, crearDatos()));

        assertTrue(error.getMessage().contains(
                "Los datos de la solicitud no son validos"));
    }

    //Prueba 5: comprobar el mensaje de falta de permisos.
    @Test
    public void rechazarUsuarioSinPermiso() {

        codigoRespuesta = 403;
        respuesta = "{\"error\":\"SIN_PERMISO\"}";

        SecurityException error = assertThrows(
                SecurityException.class,
                () -> servicio.crearSolicitud(
                        medico, crearDatos()));

        assertTrue(error.getMessage().contains(
                "No tienes permiso"));
    }

    //Prueba 6: comprobar el mensaje de solicitud inexistente.
    @Test
    public void solicitudNoEncontrada() {

        codigoRespuesta = 404;
        respuesta = "{\"error\":\"NO_ENCONTRADO\"}";

        NoSuchElementException error = assertThrows(
                NoSuchElementException.class,
                () -> servicio.consultarSolicitud(
                        medico, 999));

        assertTrue(error.getMessage().contains(
                "No se ha encontrado"));
    }

    //Prueba 7: comprobar los errores internos del servidor
    @Test
    public void controlarErrorDelServidor() {

        codigoRespuesta = 500;
        respuesta = "{\"error\":\"ERROR_INTERNO\"}";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.consultarTiposPrueba(
                        medico));
    }
}
