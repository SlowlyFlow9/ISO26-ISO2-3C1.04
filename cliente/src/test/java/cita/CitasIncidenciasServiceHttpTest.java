package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

class CitasIncidenciasServiceHttpTest {

    private static final Usuario MEDICO =
            new Usuario("medico1", Rol.MEDICO, 30L);
    private static final String INCIDENCIA_JSON =
            "{\"id\":1,\"citaId\":9,\"medicoId\":30,"
            + "\"fechaHora\":\"2026-11-27T17:00\","
            + "\"descripcion\":\"Dolor\"}";

    private HttpServer servidor;
    private CitasIncidenciasServiceHttp servicio;
    private int codigo;
    private String cuerpoRespuesta;
    private String metodoRecibido;
    private String rutaRecibida;
    private String usuarioRecibido;
    private String rolRecibido;
    private String medicoIdRecibido;
    private String cuerpoRecibido;

    @BeforeEach
    void preparar() throws IOException {
        codigo = 201;
        cuerpoRespuesta = INCIDENCIA_JSON;
        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/citas", this::atender);
        servidor.start();
        servicio = new CitasIncidenciasServiceHttp(
                "http://127.0.0.1:"
                        + servidor.getAddress().getPort());
    }

    private void atender(HttpExchange exchange)
            throws IOException {
        metodoRecibido = exchange.getRequestMethod();
        rutaRecibida = exchange.getRequestURI().getPath();
        usuarioRecibido = exchange.getRequestHeaders()
                .getFirst("X-Usuario");
        rolRecibido = exchange.getRequestHeaders()
                .getFirst("X-Rol");
        medicoIdRecibido = exchange.getRequestHeaders()
                .getFirst("X-Medico-Id");
        cuerpoRecibido = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);

        byte[] respuesta = cuerpoRespuesta.getBytes(
                StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json");
        exchange.sendResponseHeaders(codigo, respuesta.length);
        try (var salida = exchange.getResponseBody()) {
            salida.write(respuesta);
        }
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void registrarEnvíaIdentidadYDescripcion()
            throws Exception {
        IncidenciaCita incidencia =
                servicio.registrarIncidencia(
                        MEDICO, 9L, "Dolor");

        assertEquals(1L, incidencia.getId());
        assertEquals(9L, incidencia.getCitaId());
        assertEquals(
                LocalDateTime.of(2026, 11, 27, 17, 0),
                incidencia.getFechaHora());
        assertEquals("POST", metodoRecibido);
        assertEquals("/citas/9/incidencias", rutaRecibida);
        assertEquals("medico1", usuarioRecibido);
        assertEquals("MEDICO", rolRecibido);
        assertEquals("30", medicoIdRecibido);
        assertEquals("{\"descripcion\":\"Dolor\"}",
                cuerpoRecibido);
    }

    @Test
    void consultarDevuelveElHistorial()
            throws Exception {
        codigo = 200;
        cuerpoRespuesta = "[" + INCIDENCIA_JSON + "]";

        var historial = servicio.listarIncidencias(MEDICO, 9L);

        assertEquals(1, historial.size());
        assertEquals("Dolor", historial.get(0).getDescripcion());
        assertEquals("GET", metodoRecibido);
        assertEquals("/citas/9/incidencias", rutaRecibida);
        assertEquals("30", medicoIdRecibido);
    }

    @Test
    void traduceErroresDelServidorYValidaLosDatos()
            throws Exception {
        codigo = 403;
        cuerpoRespuesta = "{\"mensaje\":\"No autorizado\"}";
        assertThrows(
                SinPermisoException.class,
                () -> servicio.listarIncidencias(MEDICO, 9L));

        codigo = 404;
        cuerpoRespuesta = "{\"mensaje\":\"No existe la cita\"}";
        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio.listarIncidencias(MEDICO, 9L));

        codigo = 409;
        assertThrows(
                CitaNoIncidenciableException.class,
                () -> servicio.registrarIncidencia(
                        MEDICO, 9L, "Dolor"));

        codigo = 201;
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrarIncidencia(
                        MEDICO, 9L, "  "));
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.listarIncidencias(MEDICO, 0));
        assertThrows(
                SinPermisoException.class,
                () -> servicio.listarIncidencias(
                        new Usuario("paciente", Rol.PACIENTE), 9L));
    }

    @Test
    void jsonDeRespuestaInvalidoSeInformaComoErrorDeServicio() {
        cuerpoRespuesta = "no-json";
        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.registrarIncidencia(
                        MEDICO, 9L, "Dolor"));
    }
}
