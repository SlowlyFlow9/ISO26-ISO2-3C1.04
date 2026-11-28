package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

/** CU04: comunicación HTTP de la cancelación de citas. */
class CitasServiceHttpCancelacionTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private static final String CITA_JSON =
            "{\"id\":1,\"pacienteId\":10,"
            + "\"medicoId\":30,\"tipoPruebaId\":40,"
            + "\"fechaHora\":\"2026-12-10T10:00\","
            + "\"estado\":\"CANCELADA\"}";

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

        byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);

        peticion.getResponseHeaders().set(
                "Content-Type", "application/json");
        peticion.sendResponseHeaders(codigo, datos.length);

        try (var salida = peticion.getResponseBody()) {
            salida.write(datos);
        }
    }

    @AfterEach
    void cerrar() {
        servidor.stop(0);
    }

    @Test
    void cancelarCitaPorHttp() throws Exception {

        Cita cancelada = servicio.cancelarMiCita(PACIENTE, 1L);

        assertEquals("POST", metodoRecibido);
        assertEquals("/citas/1/cancelar", rutaRecibida);
        assertEquals("paciente1", usuarioRecibido);
        assertEquals("PACIENTE", rolRecibido);
        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
    }

    @Test
    void rechazarUsuarioSinPermiso() {

        codigo = 403;
        cuerpo = "{\"error\":\"SIN_PERMISO\","
                + "\"mensaje\":\"Sin permiso\"}";

        SinPermisoException error = assertThrows(
                SinPermisoException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 1L));

        assertEquals("Sin permiso", error.getMessage());
    }

    @Test
    void rechazarCitaInexistente() {

        codigo = 404;
        cuerpo = "{\"error\":\"NO_ENCONTRADA\"}";

        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 999L));
    }

    @Test
    void rechazarCitaNoCancelable() {

        codigo = 409;
        cuerpo = "{\"error\":\"NO_CANCELABLE\","
                + "\"mensaje\":\"La cita ya está CANCELADA\"}";

        CitaNoModificableException error = assertThrows(
                CitaNoModificableException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 1L));

        assertEquals("La cita ya está CANCELADA", error.getMessage());
    }

    @Test
    void rechazarCancelacionFueraDePlazo() {

        codigo = 409;
        cuerpo = "{\"error\":\"FUERA_DE_PLAZO\","
                + "\"mensaje\":\"Faltan menos de 24 horas\"}";

        CancelacionFueraDePlazoException error = assertThrows(
                CancelacionFueraDePlazoException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 1L));

        assertEquals("Faltan menos de 24 horas", error.getMessage());
    }

    @Test
    void conflictoDesconocidoNoSeTrataComoCancelacion() {

        codigo = 409;
        cuerpo = "{\"error\":\"OTRO\"}";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 1L));
    }

    @Test
    void conflictoSinCuerpoJsonNoSeTrataComoCancelacion() {

        codigo = 409;
        cuerpo = "no es json";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 1L));
    }

    @Test
    void datosInvalidosDelServidor() {

        codigo = 400;
        cuerpo = "{\"error\":\"DATOS_INVALIDOS\","
                + "\"mensaje\":\"Identificador inválido\"}";

        assertThrows(IllegalArgumentException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 1L));
    }

    @Test
    void rechazarRespuestaInesperada() {

        codigo = 500;
        cuerpo = "{\"error\":\"ERROR_INTERNO\"}";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 1L));
    }

    @Test
    void rechazarCitaInvalidaEnLaRespuesta() {

        cuerpo = "{\"id\":1}";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 1L));
    }

    @Test
    void rechazarIdentificadorInvalido() {

        assertThrows(IllegalArgumentException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 0L));
    }

    @Test
    void rechazarUsuarioNuloSinLlamarAlServidor() {

        assertThrows(SinPermisoException.class,
                () -> servicio.cancelarMiCita(null, 1L));
    }
}
