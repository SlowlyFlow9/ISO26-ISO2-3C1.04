package cita;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HistorialPruebasServidorHttpTest {

    private HttpServer servidor;
    private String url;
    private CitasRepositorioMemoria repositorio;
    private IdentidadPacienteMemoria identidad;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @BeforeEach
    void preparar() throws Exception {

        repositorio = new CitasRepositorioMemoria();
        identidad = new IdentidadPacienteMemoria();
        identidad.vincular("paciente1", 10L);

        CitasService citas =
                new CitasServiceImpl(repositorio, identidad);

        HistorialPruebasService historial =
                new HistorialPruebasServiceImpl(citas);

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        HistorialPruebasServidorHttp.registrar(
                servidor, historial);

        servidor.start();

        url = "http://127.0.0.1:"
                + servidor.getAddress().getPort();
    }

    private Cita crearCita(
            long pacienteId, EstadoCita estado) {

        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setMedicoId(20L);
        cita.setTipoPruebaId(30L);
        cita.setFechaHora(
                LocalDateTime.of(2026, 11, 20, 10, 0));
        cita.setEstado(estado);

        return repositorio.guardar(cita);
    }

    private HttpResponse<String> pedir(
            String metodo,
            String ruta,
            String usuario,
            String rol) throws Exception {

        var peticion = HttpRequest.newBuilder(
                URI.create(url + ruta));

        if (usuario != null) {
            peticion.header("X-Usuario", usuario);
        }

        if (rol != null) {
            peticion.header("X-Rol", rol);
        }

        return cliente.send(
                peticion.method(
                        metodo,
                        HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @AfterEach
    void cerrar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void consultarHistorialCorrectamente() throws Exception {

        crearCita(10L, EstadoCita.REALIZADA);

        var respuesta = pedir(
                "GET", "/historial-pruebas",
                "paciente1", "PACIENTE");

        assertEquals(200, respuesta.statusCode());

        var historial = JsonParser.parseString(
                respuesta.body()).getAsJsonArray();

        assertEquals(1, historial.size());
        assertEquals("REALIZADA",
                historial.get(0).getAsJsonObject()
                        .get("estado").getAsString());
    }

    @Test
    void excluirCitasNoRealizadas() throws Exception {

        crearCita(10L, EstadoCita.PROGRAMADA);
        crearCita(10L, EstadoCita.CONFIRMADA);
        crearCita(10L, EstadoCita.CANCELADA);
        crearCita(10L, EstadoCita.REALIZADA);

        var respuesta = pedir(
                "GET", "/historial-pruebas",
                "paciente1", "PACIENTE");

        var historial = JsonParser.parseString(
                respuesta.body()).getAsJsonArray();

        assertEquals(1, historial.size());
    }

    @Test
    void noMostrarCitasAjenas() throws Exception {

        crearCita(20L, EstadoCita.REALIZADA);

        var respuesta = pedir(
                "GET", "/historial-pruebas",
                "paciente1", "PACIENTE");

        assertEquals("[]", respuesta.body());
    }

    @Test
    void historialVacio() throws Exception {

        var respuesta = pedir(
                "GET", "/historial-pruebas",
                "paciente1", "PACIENTE");

        assertEquals(200, respuesta.statusCode());
        assertEquals("[]", respuesta.body());
    }

    @Test
    void rechazarRolIncorrecto() throws Exception {

        var respuesta = pedir(
                "GET", "/historial-pruebas",
                "medico1", "MEDICO");

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarPacienteNoVinculado() throws Exception {

        var respuesta = pedir(
                "GET", "/historial-pruebas",
                "desconocido", "PACIENTE");

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarUsuarioSinCabeceras() throws Exception {

        var respuesta = pedir(
                "GET", "/historial-pruebas",
                null, null);

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarMetodoIncorrecto() throws Exception {

        var respuesta = pedir(
                "POST", "/historial-pruebas",
                "paciente1", "PACIENTE");

        assertEquals(405, respuesta.statusCode());
    }

    @Test
    void rechazarRutaDesconocida() throws Exception {

        var respuesta = pedir(
                "GET", "/historial-pruebas/otro",
                "paciente1", "PACIENTE");

        assertEquals(404, respuesta.statusCode());
    }

    @Test
    void comprobarDatosDevueltos() throws Exception {

        crearCita(10L, EstadoCita.REALIZADA);

        var respuesta = pedir(
                "GET", "/historial-pruebas",
                "paciente1", "PACIENTE");

        var cita = JsonParser.parseString(respuesta.body())
                .getAsJsonArray().get(0).getAsJsonObject();

        assertEquals(10L,
                cita.get("pacienteId").getAsLong());

        assertEquals(20L,
                cita.get("medicoId").getAsLong());

        assertEquals(30L,
                cita.get("tipoPruebaId").getAsLong());

        assertEquals("2026-11-20T10:00",
                cita.get("fechaHora").getAsString());
    }
}
