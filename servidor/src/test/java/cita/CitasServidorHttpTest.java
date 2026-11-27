package cita;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;
import solicitud.SolicitudesRepositorioMemoria;

/**
 * CU02 - Pruebas de las rutas HTTP de citas.
 */

class CitasServidorHttpTest {

    private HttpServer servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();

    private CitasRepositorioMemoria repositorio;
    private SolicitudesRepositorioMemoria solicitudes;

    @BeforeEach
    void preparar() throws Exception {

        repositorio = new CitasRepositorioMemoria();
        solicitudes = new SolicitudesRepositorioMemoria();

        IdentidadPacienteMemoria identidad =
                new IdentidadPacienteMemoria();

        identidad.vincular("paciente1", 10L);
        identidad.vincular("paciente2", 20L);

        repositorio.guardar(crearCita(10L));
        repositorio.guardar(crearCita(20L));

        // Arranque compartido del proyecto.
        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(
                        new PacientesRepositorioMemoria()),
                0);

        CitasServidorHttp.registrar(
                servidor,
                new CitasServiceImpl(
                        repositorio, identidad),
                new CitasRealizacionServiceImpl(
                        repositorio, solicitudes));
    }

    @AfterEach
    void cerrar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private Cita crearCita(long pacienteId) {

        Cita cita = new Cita();

        cita.setPacienteId(pacienteId);
        cita.setMedicoId(30L);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(
                LocalDateTime.of(2026, 12, 10, 10, 0));
        cita.setEstado(EstadoCita.PROGRAMADA);

        return cita;
    }

    private HttpResponse<String> pedir(
            String ruta,
            String metodo,
            String usuario,
            String rol) throws Exception {
        return pedir(ruta, metodo, usuario, rol, null);
    }

    private HttpResponse<String> pedir(
            String ruta,
            String metodo,
            String usuario,
            String rol,
            String medicoId) throws Exception {

        String url = "http://127.0.0.1:"
                + servidor.getAddress().getPort()
                + ruta;

        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(URI.create(url));

        if (usuario != null) {
            peticion.header("X-Usuario", usuario);
        }

        if (rol != null) {
            peticion.header("X-Rol", rol);
        }
        if (medicoId != null) {
            peticion.header("X-Medico-Id", medicoId);
        }

        if ("POST".equals(metodo)) {
            peticion.POST(HttpRequest.BodyPublishers.noBody());
        } else if ("GET".equals(metodo)) {
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

    private HttpResponse<String> comoPaciente(
            String ruta,
            String metodo) throws Exception {

        return pedir(
                ruta, metodo, "paciente1", "PACIENTE");
    }

    private HttpResponse<String> comoMedico(
            String ruta,
            String metodo) throws Exception {
        return pedir(
                ruta, metodo, "medico1", "MEDICO",
                "30");
    }

    @Test
    void listarSoloCitasPropias() throws Exception {

        var respuesta = comoPaciente("/citas", "GET");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body()
                .contains("\"pacienteId\":10"));
        assertFalse(respuesta.body()
                .contains("\"pacienteId\":20"));
    }

    @Test
    void consultarCitaPropia() throws Exception {

        var respuesta = comoPaciente("/citas/1", "GET");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body()
                .contains("\"id\":1"));
    }

    @Test
    void confirmarCitaCorrectamente() throws Exception {

        var respuesta = comoPaciente(
                "/citas/1/confirmar", "POST");

        assertEquals(200, respuesta.statusCode());

        assertTrue(respuesta.body()
                .contains("\"estado\":\"CONFIRMADA\""));

        assertEquals(
                EstadoCita.CONFIRMADA,
                repositorio.buscarPorId(1L)
                        .orElseThrow().getEstado());
    }

    @Test
    void rechazarSegundaConfirmacion() throws Exception {

        comoPaciente("/citas/1/confirmar", "POST");

        var respuesta = comoPaciente(
                "/citas/1/confirmar", "POST");

        assertEquals(409, respuesta.statusCode());
    }

    @Test
    void rechazarCitaCancelada() throws Exception {

        Cita cita = repositorio.buscarPorId(1L)
                .orElseThrow().copia();

        cita.setEstado(EstadoCita.CANCELADA);
        repositorio.guardar(cita);

        var respuesta = comoPaciente(
                "/citas/1/confirmar", "POST");

        assertEquals(409, respuesta.statusCode());
    }

    @Test
    void rechazarCitaDeOtroPaciente() throws Exception {

        var respuesta = comoPaciente(
                "/citas/2/confirmar", "POST");

        assertEquals(404, respuesta.statusCode());
    }

    @Test
    void rechazarCitaInexistente() throws Exception {

        var respuesta = comoPaciente(
                "/citas/999/confirmar", "POST");

        assertEquals(404, respuesta.statusCode());
    }

    @Test
    void rechazarUsuarioSinCabeceras() throws Exception {

        var respuesta = pedir(
                "/citas/1/confirmar", "POST", null, null);

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarMedico() throws Exception {

        var respuesta = pedir(
                "/citas/1/confirmar",
                "POST", "medico1", "MEDICO");

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarPacienteSinVinculacion() throws Exception {

        var respuesta = pedir(
                "/citas/1/confirmar",
                "POST", "desconocido", "PACIENTE");

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarIdentificadorInvalido() throws Exception {

        var respuesta = comoPaciente(
                "/citas/abc/confirmar", "POST");

        assertEquals(400, respuesta.statusCode());
    }

    @Test
    void rechazarMetodoIncorrecto() throws Exception {

        var respuesta = comoPaciente(
                "/citas/1/confirmar", "GET");

        assertEquals(405, respuesta.statusCode());
    }

    @Test
    void medicoPuedeMarcarSuCitaComoRealizada()
            throws Exception {
        var respuesta = comoMedico(
                "/citas/1/realizar", "POST");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body()
                .contains("\"estado\":\"REALIZADA\""));
        assertEquals(
                EstadoCita.REALIZADA,
                repositorio.buscarPorId(1L)
                        .orElseThrow().getEstado());
    }

    @Test
    void rechazarPeticionMedicaDeOtroMedico()
            throws Exception {
        var respuesta = pedir(
                "/citas/1/realizar",
                "POST",
                "otro",
                "MEDICO",
                "31");

        assertEquals(403, respuesta.statusCode());
        assertEquals(
                EstadoCita.PROGRAMADA,
                repositorio.buscarPorId(1L)
                        .orElseThrow().getEstado());
    }

}
