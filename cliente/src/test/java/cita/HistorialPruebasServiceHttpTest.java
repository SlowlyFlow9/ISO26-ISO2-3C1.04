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
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU06: Pruebas HTTP del cliente
 */
class HistorialPruebasServiceHttpTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private static final String PRUEBA_JSON =
            "{\"id\":1,\"pacienteId\":10,"
            + "\"medicoId\":20,\"tipoPruebaId\":30,"
            + "\"fechaHora\":\"2026-11-20T10:00\","
            + "\"estado\":\"REALIZADA\"}";

    private HttpServer servidor;
    private HistorialPruebasServiceHttp servicio;

    private volatile int codigo;
    private volatile String cuerpo;
    private volatile String metodoRecibido;
    private volatile String usuarioRecibido;
    private volatile String rolRecibido;
    private volatile String rutaRecibida;

    @BeforeEach
    void preparar() throws Exception {

        codigo = 200;
        cuerpo = "[" + PRUEBA_JSON + "]";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        servidor.createContext(
                "/historial-pruebas", this::atender);

        servidor.start();

        servicio = new HistorialPruebasServiceHttp(
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

        peticion.sendResponseHeaders(codigo, datos.length);

        try (var salida = peticion.getResponseBody()) {
            salida.write(datos);
        }
    }

    @AfterEach
    void cerrar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void consultarHistorialPorHttp() throws Exception {

        var historial = servicio.listarMiHistorial(PACIENTE);

        assertEquals(1, historial.size());
        assertEquals("GET", metodoRecibido);
        assertEquals("/historial-pruebas", rutaRecibida);
        assertEquals("paciente1", usuarioRecibido);
        assertEquals("PACIENTE", rolRecibido);

        assertEquals(
                EstadoCita.REALIZADA,
                historial.get(0).getEstado());
    }

    @Test
    void comprobarDatosRecibidos() throws Exception {

        Cita cita =
                servicio.listarMiHistorial(PACIENTE).get(0);

        assertEquals(1L, cita.getId());
        assertEquals(10L, cita.getPacienteId());
        assertEquals(20L, cita.getMedicoId());
        assertEquals(30L, cita.getTipoPruebaId());

        assertEquals(
                LocalDateTime.of(2026, 11, 20, 10, 0),
                cita.getFechaHora());
    }

    @Test
    void historialVacio() throws Exception {

        cuerpo = "[]";

        assertTrue(
                servicio.listarMiHistorial(PACIENTE).isEmpty());
    }

    @Test
    void rechazarUsuarioSinPermiso() {

        codigo = 403;
        cuerpo = "{\"error\":\"SIN_PERMISO\","
                + "\"mensaje\":\"Sin permiso\"}";

        SinPermisoException error = assertThrows(
                SinPermisoException.class,
                () -> servicio.listarMiHistorial(PACIENTE));

        assertEquals("Sin permiso", error.getMessage());
    }

    @Test
    void rechazarUsuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.listarMiHistorial(null));
    }

    @Test
    void gestionarErrorServidor() {

        codigo = 500;
        cuerpo = "{\"error\":\"ERROR_INTERNO\"}";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listarMiHistorial(PACIENTE));
    }

    @Test
    void rechazarJsonInvalido() {

        cuerpo = "esto no es JSON";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listarMiHistorial(PACIENTE));
    }

    @Test
    void rechazarCitaNoRealizada() {

        cuerpo = "[" + PRUEBA_JSON.replace(
                "REALIZADA", "PROGRAMADA") + "]";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listarMiHistorial(PACIENTE));
    }

    @Test
    void rechazarDatosIncompletos() {

        cuerpo = "[{\"id\":1,\"estado\":\"REALIZADA\"}]";

        assertThrows(
                ServicioNoDisponibleException.class,
                () -> servicio.listarMiHistorial(PACIENTE));
    }
}
