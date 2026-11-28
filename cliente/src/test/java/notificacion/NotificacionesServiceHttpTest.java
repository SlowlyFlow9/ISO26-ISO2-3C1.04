package notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

class NotificacionesServiceHttpTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private static final String NOTIFICACION_JSON =
            "{\"id\":1,\"pacienteId\":10,"
            + "\"titulo\":\"Cita programada\","
            + "\"mensaje\":\"Tiene una cita pendiente.\","
            + "\"fechaHora\":\"2026-11-28T10:00\","
            + "\"leida\":false}";

    private HttpServer servidor;
    private NotificacionesServiceHttp servicio;

    private volatile int codigo;
    private volatile String cuerpo;
    private volatile String metodoRecibido;
    private volatile String rutaRecibida;
    private volatile String usuarioRecibido;
    private volatile String rolRecibido;

    @BeforeEach
    void preparar() throws IOException {
        codigo = 200;
        cuerpo = "[" + NOTIFICACION_JSON + "]";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(
                "/notificaciones", this::atender);
        servidor.start();

        servicio = new NotificacionesServiceHttp(
                "http://127.0.0.1:"
                        + servidor.getAddress().getPort());
    }

    private void atender(HttpExchange peticion)
            throws IOException {
        metodoRecibido = peticion.getRequestMethod();
        rutaRecibida = peticion.getRequestURI().getPath();
        usuarioRecibido = peticion.getRequestHeaders()
                .getFirst("X-Usuario");
        rolRecibido = peticion.getRequestHeaders()
                .getFirst("X-Rol");

        byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);
        peticion.getResponseHeaders().set(
                "Content-Type", "application/json");
        peticion.sendResponseHeaders(codigo, datos.length);
        try (var salida = peticion.getResponseBody()) {
            salida.write(datos);
        }
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void listarNotificacionesPorHttp() throws Exception {
        var resultado = servicio.listarMisNotificaciones(PACIENTE);

        assertEquals(1, resultado.size());
        Notificacion notificacion = resultado.get(0);
        assertEquals(Long.valueOf(1L), notificacion.getId());
        assertEquals("Cita programada", notificacion.getTitulo());
        assertEquals("Tiene una cita pendiente.",
                notificacion.getMensaje());
        assertEquals(
                LocalDateTime.of(2026, 11, 28, 10, 0),
                notificacion.getFechaHora());
        assertEquals(Boolean.FALSE, notificacion.getLeida());
        assertEquals("GET", metodoRecibido);
        assertEquals("/notificaciones", rutaRecibida);
        assertEquals("paciente1", usuarioRecibido);
        assertEquals("PACIENTE", rolRecibido);
    }

    @Test
    void listarSinNotificaciones() throws Exception {
        cuerpo = "[]";

        assertTrue(
                servicio.listarMisNotificaciones(PACIENTE).isEmpty());
    }

    @Test
    void propagarErrorDePermisoDelServidor() {
        codigo = 403;
        cuerpo = "{\"error\":\"SIN_PERMISO\","
                + "\"mensaje\":\"Solo pacientes\"}";

        SinPermisoException error = assertThrows(
                SinPermisoException.class,
                () -> servicio.listarMisNotificaciones(PACIENTE));

        assertEquals("Solo pacientes", error.getMessage());
    }

    @Test
    void gestionarRespuestaHttpInesperada() {
        codigo = 500;
        cuerpo = "{\"error\":\"ERROR_INTERNO\"}";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listarMisNotificaciones(PACIENTE));
    }

    @Test
    void rechazarListadoJsonInvalido() {
        cuerpo = "[{\"id\":1,\"titulo\":\"Sin fecha\"}]";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listarMisNotificaciones(PACIENTE));
    }

    @Test
    void rechazarJsonMalformado() {
        cuerpo = "{";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listarMisNotificaciones(PACIENTE));
    }

    @Test
    void rechazarUsuarioNuloAntesDeLaPeticion() {
        assertThrows(
                SinPermisoException.class,
                () -> servicio.listarMisNotificaciones(null));
    }
}
