package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

class CitasRealizacionServiceHttpTest {

    private static final Usuario MEDICO =
            new Usuario("medico1", Rol.MEDICO, 30L);
    private static final String CITA_JSON =
            "{\"id\":1,\"pacienteId\":10,\"medicoId\":30,"
            + "\"tipoPruebaId\":40,"
            + "\"fechaHora\":\"2026-11-27T10:00\","
            + "\"estado\":\"REALIZADA\"}";

    private HttpServer servidor;
    private CitasRealizacionServiceHttp servicio;
    private int codigo;
    private String cuerpoRespuesta;
    private String metodoRecibido;
    private String rutaRecibida;
    private String usuarioRecibido;
    private String rolRecibido;
    private String medicoIdRecibido;

    @BeforeEach
    void preparar() throws IOException {
        codigo = 200;
        cuerpoRespuesta = CITA_JSON;
        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/citas", this::atender);
        servidor.start();
        servicio = new CitasRealizacionServiceHttp(
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
    void marcarRealizadaEnvíaLaIdentidadDelMedico()
            throws Exception {
        Cita cita = servicio.marcarRealizada(MEDICO, 1L);

        assertEquals(EstadoCita.REALIZADA, cita.getEstado());
        assertEquals("POST", metodoRecibido);
        assertEquals("/citas/1/realizar", rutaRecibida);
        assertEquals("medico1", usuarioRecibido);
        assertEquals("MEDICO", rolRecibido);
        assertEquals("30", medicoIdRecibido);
    }

    @Test
    void convierteErroresHttpEnExcepcionesDelServicio() {
        codigo = 403;
        cuerpoRespuesta =
                "{\"error\":\"SIN_PERMISO\","
                + "\"mensaje\":\"No autorizado\"}";
        assertThrows(
                SinPermisoException.class,
                () -> servicio.marcarRealizada(MEDICO, 1L));

        codigo = 409;
        cuerpoRespuesta =
                "{\"error\":\"NO_REALIZABLE\","
                + "\"mensaje\":\"Estado no válido\"}";
        assertThrows(
                CitaNoRealizableException.class,
                () -> servicio.marcarRealizada(MEDICO, 1L));
    }

    @Test
    void rechazaIdentificadoresYMedicosInvalidos() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.marcarRealizada(MEDICO, 0));
        assertThrows(
                SinPermisoException.class,
                () -> servicio.marcarRealizada(
                        new Usuario("medico", Rol.MEDICO), 1L));
    }

    @Test
    void respuestaJsonInvalidaSeInformaComoErrorDeServicio() {
        cuerpoRespuesta = "no-json";
        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.marcarRealizada(MEDICO, 1L));
    }
}
