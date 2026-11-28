package notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cita.IdentidadPacienteMemoria;
import com.sun.net.httpserver.HttpServer;
import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;

class NotificacionesServidorHttpTest {

    private final HttpClient cliente = HttpClient.newHttpClient();
    private HttpServer servidor;

    @BeforeEach
    void preparar() throws Exception {
        var repositorio = new NotificacionesRepositorioMemoria();
        repositorio.guardar(new Notificacion(
                1L,
                10L,
                "Cita programada",
                "Tiene una cita pendiente.",
                LocalDateTime.of(2026, 11, 28, 10, 0),
                false));
        repositorio.guardar(new Notificacion(
                2L,
                20L,
                "Privada",
                "No debe aparecer en la respuesta.",
                LocalDateTime.of(2026, 11, 27, 9, 0),
                true));

        var identidad = new IdentidadPacienteMemoria();
        identidad.vincular("paciente1", 10L);
        identidad.vincular("paciente2", 20L);

        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(
                        new PacientesRepositorioMemoria()),
                0);
        NotificacionesServidorHttp.registrar(
                servidor,
                new NotificacionesServiceImpl(
                        repositorio,
                        identidad));
    }

    @AfterEach
    void cerrar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private HttpResponse<String> pedir(
            String ruta,
            String metodo,
            String usuario,
            String rol) throws Exception {

        var peticion = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:"
                        + servidor.getAddress().getPort()
                        + ruta));

        if (usuario != null) {
            peticion.header("X-Usuario", usuario);
        }
        if (rol != null) {
            peticion.header("X-Rol", rol);
        }

        if ("GET".equals(metodo)) {
            peticion.GET();
        } else {
            peticion.method(
                    metodo,
                    HttpRequest.BodyPublishers.noBody());
        }

        return cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> comoPaciente(String ruta)
            throws Exception {
        return pedir(ruta, "GET", "paciente1", "PACIENTE");
    }

    @Test
    void listarNotificacionesPropiasEnJson() throws Exception {
        var respuesta = comoPaciente("/notificaciones");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("\"titulo\":\"Cita programada\""));
        assertTrue(respuesta.body().contains(
                "\"mensaje\":\"Tiene una cita pendiente.\""));
        assertTrue(respuesta.body().contains(
                "\"fechaHora\":\"2026-11-28T10:00\""));
        assertTrue(respuesta.body().contains("\"leida\":false"));
        assertFalse(respuesta.body().contains("No debe aparecer"));
    }

    @Test
    void parametroNoPermiteConsultarNotificacionesDeOtroPaciente()
            throws Exception {
        var respuesta = comoPaciente(
                "/notificaciones?pacienteId=20");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("\"pacienteId\":10"));
        assertFalse(respuesta.body().contains("\"pacienteId\":20"));
        assertFalse(respuesta.body().contains("No debe aparecer"));
    }

    @Test
    void rechazaUsuarioSinIdentidad() throws Exception {
        var respuesta = pedir(
                "/notificaciones", "GET", null, null);

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazaRolDistintoDePaciente() throws Exception {
        var respuesta = pedir(
                "/notificaciones", "GET", "medico", "MEDICO");

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazaPacienteSinVinculacion() throws Exception {
        var respuesta = pedir(
                "/notificaciones", "GET", "desconocido", "PACIENTE");

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazaMetodosDistintosDeGet() throws Exception {
        var respuesta = pedir(
                "/notificaciones", "POST", "paciente1", "PACIENTE");

        assertEquals(405, respuesta.statusCode());
    }

    @Test
    void noExponeOperacionDeDetalle() throws Exception {
        var respuesta = comoPaciente("/notificaciones/1");

        assertEquals(404, respuesta.statusCode());
    }
}
