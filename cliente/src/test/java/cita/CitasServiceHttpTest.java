package cita;

import static org.junit.jupiter.api.Assertions.*;

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
import paciente.SinPermisoException;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

/**
 * CU02 - Pruebas de comunicacion HTTP del cliente.
 */

class CitasServiceHttpTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private static final String CITA_JSON =
            "{\"id\":1,\"pacienteId\":10,"
            + "\"medicoId\":30,\"tipoPruebaId\":40,"
            + "\"fechaHora\":\"2026-12-10T10:00\","
            + "\"estado\":\"PROGRAMADA\"}";

    private HttpServer servidor;
    private CitasServiceHttp servicio;

    private volatile int codigo;
    private volatile String cuerpo;
    private volatile String metodoRecibido;
    private volatile String rutaRecibida;
    private volatile String usuarioRecibido;
    private volatile String rolRecibido;

    @BeforeEach
    void preparar() throws IOException {

        codigo = 200;
        cuerpo = CITA_JSON;

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext("/citas", this::atender);
        servidor.start();

        servicio = new CitasServiceHttp(
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

        byte[] datos = cuerpo.getBytes(
                StandardCharsets.UTF_8);

        peticion.getResponseHeaders().set(
                "Content-Type", "application/json");

        peticion.sendResponseHeaders(
                codigo, datos.length);

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
    void listarCitasPorHttp() throws Exception {

        cuerpo = "[" + CITA_JSON + "]";

        var citas = servicio.listarMisCitas(PACIENTE);

        assertEquals(1, citas.size());
        assertEquals(EstadoCita.PROGRAMADA,
                citas.get(0).getEstado());
        assertEquals("GET", metodoRecibido);
        assertEquals("/citas", rutaRecibida);
        assertEquals("paciente1", usuarioRecibido);
        assertEquals("PACIENTE", rolRecibido);
    }

    @Test
    void listarSinCitas() throws Exception {

        cuerpo = "[]";

        assertTrue(
                servicio.listarMisCitas(PACIENTE).isEmpty());
    }

    @Test
    void consultarCitaPorHttp() throws Exception {

        Cita cita = servicio.consultarMiCita(
                PACIENTE, 1L);

        assertEquals("/citas/1", rutaRecibida);
        assertEquals("GET", metodoRecibido);
        assertEquals(Long.valueOf(1L), cita.getId());

        assertEquals(
                LocalDateTime.of(2026, 12, 10, 10, 0),
                cita.getFechaHora());
    }

    @Test
    void confirmarCitaPorHttp() throws Exception {

        cuerpo = CITA_JSON.replace(
                "PROGRAMADA", "CONFIRMADA");

        Cita confirmada = servicio.confirmarMiCita(
                PACIENTE, 1L);

        assertEquals("POST", metodoRecibido);
        assertEquals("/citas/1/confirmar", rutaRecibida);
        assertEquals("paciente1", usuarioRecibido);
        assertEquals("PACIENTE", rolRecibido);

        assertEquals(
                EstadoCita.CONFIRMADA,
                confirmada.getEstado());
    }

    @Test
    void rechazarUsuarioSinPermiso() {

        codigo = 403;
        cuerpo = "{\"error\":\"SIN_PERMISO\","
                + "\"mensaje\":\"Sin permiso\"}";

        SinPermisoException error = assertThrows(
                SinPermisoException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, 1L));

        assertEquals("Sin permiso", error.getMessage());
    }

    @Test
    void rechazarCitaInexistente() {

        codigo = 404;
        cuerpo = "{\"error\":\"NO_ENCONTRADA\"}";

        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, 999L));
    }

    @Test
    void rechazarCitaNoConfirmable() {

        codigo = 409;
        cuerpo = "{\"error\":\"NO_CONFIRMABLE\","
                + "\"mensaje\":\"Cita ya confirmada\"}";

        assertThrows(
                CitaNoConfirmableException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, 1L));
    }

    @Test
    void rechazarIdentificadorInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, 0L));
    }

    @Test
    void rechazarUsuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.confirmarMiCita(
                        null, 1L));
    }

    @Test
    void gestionarError400() {

        codigo = 400;
        cuerpo = "{\"error\":\"DATOS_INVALIDOS\"}";

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, 1L));
    }

    @Test
    void gestionarError500() {

        codigo = 500;
        cuerpo = "{\"error\":\"ERROR_INTERNO\"}";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, 1L));
    }

    @Test
    void rechazarRespuestaJsonInvalida()
            throws Exception {

        cuerpo = "esto no es JSON";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.consultarMiCita(
                        PACIENTE, 1L));
    }
}
