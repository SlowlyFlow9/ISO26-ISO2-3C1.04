package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

/** CU03: comunicación HTTP de los huecos libres y del cambio de cita. */
class CitasServiceHttpCambioTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private static final String CITA_JSON =
            "{\"id\":1,\"pacienteId\":10,"
            + "\"medicoId\":8,\"tipoPruebaId\":40,"
            + "\"fechaHora\":\"2026-12-10T10:00\","
            + "\"estado\":\"PROGRAMADA\",\"huecoId\":200}";

    private static final String HUECOS_JSON =
            "[{\"id\":200,\"medicoId\":8,"
            + "\"fechaHora\":\"2026-12-10T10:00:00\"},"
            + "{\"id\":201,\"medicoId\":8,"
            + "\"fechaHora\":\"2026-12-10T11:00:00\"}]";

    private HttpServer servidor;
    private CitasServiceHttp servicio;

    private volatile int codigo;
    private volatile String cuerpo;
    private volatile String metodoRecibido;
    private volatile String rutaRecibida;
    private volatile String cuerpoRecibido;
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
        cuerpoRecibido = new String(
                peticion.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);

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

    // ---------- huecos libres ----------

    @Test
    void listarHuecosLibresPorHttp() throws Exception {

        cuerpo = HUECOS_JSON;

        List<HuecoLibre> huecos =
                servicio.listarHuecosLibres(PACIENTE);

        assertEquals("GET", metodoRecibido);
        assertEquals("/citas/huecos-libres", rutaRecibida);
        assertEquals("paciente1", usuarioRecibido);
        assertEquals("PACIENTE", rolRecibido);

        assertEquals(2, huecos.size());
        assertEquals(200L, huecos.get(0).id());
        assertEquals(8L, huecos.get(0).medicoId());
        assertEquals(LocalDateTime.of(2026, 12, 10, 10, 0),
                huecos.get(0).fechaHora());
    }

    @Test
    void listarHuecosVacioEsValido() throws Exception {

        cuerpo = "[]";

        assertTrue(servicio.listarHuecosLibres(PACIENTE).isEmpty());
    }

    @Test
    void listarHuecosSinPermiso() {

        codigo = 403;
        cuerpo = "{\"error\":\"SIN_PERMISO\","
                + "\"mensaje\":\"Sin permiso\"}";

        assertThrows(SinPermisoException.class,
                () -> servicio.listarHuecosLibres(PACIENTE));
    }

    @Test
    void listarHuecosRespuestaInesperada() {

        codigo = 500;
        cuerpo = "{\"error\":\"ERROR_INTERNO\"}";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listarHuecosLibres(PACIENTE));
    }

    @Test
    void listarHuecosRechazaJsonInvalido() {

        cuerpo = "no es json";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listarHuecosLibres(PACIENTE));
    }

    @Test
    void listarHuecosRechazaHuecoIncompleto() {

        cuerpo = "[{\"id\":200,\"medicoId\":8}]";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listarHuecosLibres(PACIENTE));
    }

    @Test
    void listarHuecosRechazaHuecoNulo() {

        cuerpo = "[null]";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listarHuecosLibres(PACIENTE));
    }

    // ---------- cambio de cita ----------

    @Test
    void cambiarCitaPorHttp() throws Exception {

        Cita cambiada = servicio.cambiarMiCita(PACIENTE, 1L, 200L);

        assertEquals("POST", metodoRecibido);
        assertEquals("/citas/1/cambiar", rutaRecibida);
        assertEquals("{\"huecoId\":200}", cuerpoRecibido);
        assertEquals("paciente1", usuarioRecibido);
        assertEquals("PACIENTE", rolRecibido);
        assertEquals(200L, cambiada.getHuecoId());
        assertEquals(EstadoCita.PROGRAMADA, cambiada.getEstado());
    }

    @Test
    void cambiarSinPermiso() {

        codigo = 403;
        cuerpo = "{\"error\":\"SIN_PERMISO\","
                + "\"mensaje\":\"Sin permiso\"}";

        SinPermisoException error = assertThrows(
                SinPermisoException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 200L));

        assertEquals("Sin permiso", error.getMessage());
    }

    @Test
    void cambiarCitaInexistente() {

        codigo = 404;
        cuerpo = "{\"error\":\"NO_ENCONTRADA\"}";

        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 999L, 200L));
    }

    @Test
    void cambiarAHuecoInexistente() {

        codigo = 404;
        cuerpo = "{\"error\":\"HUECO_NO_ENCONTRADO\","
                + "\"mensaje\":\"No existe el hueco 999\"}";

        HuecoNoDisponibleException error = assertThrows(
                HuecoNoDisponibleException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 999L));

        assertEquals("No existe el hueco 999", error.getMessage());
    }

    @Test
    void cambiarAHuecoNoDisponible() {

        codigo = 409;
        cuerpo = "{\"error\":\"HUECO_NO_DISPONIBLE\","
                + "\"mensaje\":\"El hueco 201 está bloqueado\"}";

        assertThrows(HuecoNoDisponibleException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 201L));
    }

    @Test
    void cambiarFueraDePolitica() {

        codigo = 409;
        cuerpo = "{\"error\":\"FUERA_DE_POLITICA\","
                + "\"mensaje\":\"Faltan menos de 24 horas\"}";

        CambioFueraDePoliticaException error = assertThrows(
                CambioFueraDePoliticaException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 200L));

        assertEquals("Faltan menos de 24 horas", error.getMessage());
    }

    @Test
    void cambiarCitaNoCambiable() {

        codigo = 409;
        cuerpo = "{\"error\":\"NO_CAMBIABLE\","
                + "\"mensaje\":\"La cita está CANCELADA\"}";

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 200L));
    }

    @Test
    void conflictoDesconocidoNoSeTrataComoCambio() {

        codigo = 409;
        cuerpo = "{\"error\":\"OTRO\"}";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 200L));
    }

    @Test
    void datosInvalidosDelServidor() {

        codigo = 400;
        cuerpo = "{\"error\":\"DATOS_INVALIDOS\","
                + "\"mensaje\":\"Cuerpo inválido\"}";

        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 200L));
    }

    @Test
    void cambiarRechazaCitaInvalidaEnLaRespuesta() {

        cuerpo = "{\"id\":1}";

        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 200L));
    }

    @Test
    void cambiarRechazaIdentificadoresInvalidos() {

        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 0L, 200L));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarMiCita(PACIENTE, 1L, 0L));
    }

    @Test
    void cambiarRechazaUsuarioNuloSinLlamarAlServidor() {

        assertThrows(SinPermisoException.class,
                () -> servicio.cambiarMiCita(null, 1L, 200L));
    }
}
