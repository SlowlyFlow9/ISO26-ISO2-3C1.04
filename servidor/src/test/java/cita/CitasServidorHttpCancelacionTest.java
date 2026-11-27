package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/** CU04: ruta HTTP de cancelación de citas del paciente. */
class CitasServidorHttpCancelacionTest {

    private HttpServer servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();

    private CitasRepositorioMemoria repositorio;

    @BeforeEach
    void preparar() throws Exception {

        repositorio = new CitasRepositorioMemoria();

        IdentidadPacienteMemoria identidad =
                new IdentidadPacienteMemoria();
        identidad.vincular("paciente1", 10L);
        identidad.vincular("paciente2", 20L);

        //cita 1: del paciente 1, dentro de 10 días
        repositorio.guardar(crearCita(10L,
                LocalDateTime.now().plusDays(10)));
        //cita 2: del paciente 2
        repositorio.guardar(crearCita(20L,
                LocalDateTime.now().plusDays(10)));
        //cita 3: del paciente 1, dentro de 2 horas (fuera de plazo)
        repositorio.guardar(crearCita(10L,
                LocalDateTime.now().plusHours(2)));

        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(
                        new PacientesRepositorioMemoria()),
                0);

        CitasServidorHttp.registrar(
                servidor,
                ServiciosCitasPrueba.citas(repositorio, identidad));
    }

    @AfterEach
    void cerrar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private Cita crearCita(long pacienteId, LocalDateTime fecha) {

        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setMedicoId(30L);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(fecha);
        cita.setEstado(EstadoCita.PROGRAMADA);

        return cita;
    }

    private HttpResponse<String> pedir(
            String ruta,
            String metodo,
            String usuario,
            String rol) throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:"
                        + servidor.getAddress().getPort() + ruta));

        if (usuario != null) {
            peticion.header("X-Usuario", usuario);
        }

        if (rol != null) {
            peticion.header("X-Rol", rol);
        }

        peticion.method(metodo,
                HttpRequest.BodyPublishers.noBody());

        return cliente.send(peticion.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> comoPaciente(
            String ruta, String metodo) throws Exception {

        return pedir(ruta, metodo, "paciente1", "PACIENTE");
    }

    @Test
    void cancelarCitaCorrectamente() throws Exception {

        var respuesta = comoPaciente("/citas/1/cancelar", "POST");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body()
                .contains("\"estado\":\"CANCELADA\""));
        assertEquals(EstadoCita.CANCELADA,
                repositorio.buscarPorId(1L).orElseThrow().getEstado());
    }

    @Test
    void rechazarSegundaCancelacion() throws Exception {

        comoPaciente("/citas/1/cancelar", "POST");

        var respuesta = comoPaciente("/citas/1/cancelar", "POST");

        assertEquals(409, respuesta.statusCode());
        assertTrue(respuesta.body().contains("NO_CANCELABLE"));
    }

    @Test
    void rechazarCancelacionFueraDePlazo() throws Exception {

        var respuesta = comoPaciente("/citas/3/cancelar", "POST");

        assertEquals(409, respuesta.statusCode());
        assertTrue(respuesta.body().contains("FUERA_DE_PLAZO"));
        assertEquals(EstadoCita.PROGRAMADA,
                repositorio.buscarPorId(3L).orElseThrow().getEstado());
    }

    @Test
    void rechazarCitaDeOtroPaciente() throws Exception {

        var respuesta = comoPaciente("/citas/2/cancelar", "POST");

        assertEquals(404, respuesta.statusCode());
    }

    @Test
    void rechazarCitaInexistente() throws Exception {

        var respuesta = comoPaciente("/citas/999/cancelar", "POST");

        assertEquals(404, respuesta.statusCode());
    }

    @Test
    void rechazarUsuarioSinCabeceras() throws Exception {

        var respuesta = pedir("/citas/1/cancelar", "POST", null, null);

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarMedico() throws Exception {

        var respuesta = pedir(
                "/citas/1/cancelar", "POST", "medico1", "MEDICO");

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarIdentificadorInvalido() throws Exception {

        var respuesta = comoPaciente("/citas/abc/cancelar", "POST");

        assertEquals(400, respuesta.statusCode());
    }

    @Test
    void rechazarIdentificadorNoPositivo() throws Exception {

        var respuesta = comoPaciente("/citas/0/cancelar", "POST");

        assertEquals(400, respuesta.statusCode());
    }

    @Test
    void rechazarMetodoIncorrecto() throws Exception {

        var respuesta = comoPaciente("/citas/1/cancelar", "GET");

        assertEquals(405, respuesta.statusCode());
    }
}
