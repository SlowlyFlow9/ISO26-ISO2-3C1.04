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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpServer;

import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;

class PoliticasCitaServidorHttpTest {

    private HttpServer servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>)
                    (fecha, tipo, contexto) ->
                            new JsonPrimitive(fecha.toString()))
            .create();

    @BeforeEach
    void preparar() throws Exception {
        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(
                        new PacientesRepositorioMemoria()),
                0);
        PoliticaCitaRepositorioMemoria repositorio =
                new PoliticaCitaRepositorioMemoria();
        PoliticasCitaServidorHttp.registrar(
                servidor,
                new PoliticaCitaServiceImpl(repositorio));
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
            String rol,
            String cuerpo) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:"
                        + servidor.getAddress().getPort()
                        + ruta))
                .header("X-Usuario", "admin");
        if (rol != null) {
            peticion.header("X-Rol", rol);
        }
        if ("GET".equals(metodo)) {
            peticion.GET();
        } else {
            peticion.method(
                    metodo,
                    HttpRequest.BodyPublishers.ofString(
                            cuerpo == null ? "" : cuerpo));
        }
        return cliente.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private PoliticaCita politicaValida() {
        return PoliticaCita.inicial();
    }

    @Test
    void consultarValoresVigentesComoAdministrador()
            throws Exception {
        HttpResponse<String> respuesta = pedir(
                "/politicas-citacion",
                "GET",
                "ADMINISTRADOR",
                null);

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body()
                .contains("\"antelacionMaximaDias\":90"));
    }

    @Test
    void modificarPoliticaComoAdministrador()
            throws Exception {
        HttpResponse<String> respuesta = pedir(
                "/politicas-citacion",
                "PUT",
                "ADMINISTRADOR",
                gson.toJson(politicaValida()));

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body()
                .contains("\"modificadoPor\":\"admin\""));
        assertTrue(respuesta.body()
                .contains("\"maxCitasActivasPorPaciente\":3"));
    }

    @Test
    void rechazarUsuarioSinPermiso() throws Exception {
        assertEquals(403, pedir(
                "/politicas-citacion",
                "GET",
                "MEDICO",
                null).statusCode());
    }

    @Test
    void rechazarPoliticaInvalida() throws Exception {
        PoliticaCita politica = politicaValida();
        politica.setAntelacionMinimaHoras(-1);

        HttpResponse<String> respuesta = pedir(
                "/politicas-citacion",
                "PUT",
                "ADMINISTRADOR",
                gson.toJson(politica));

        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body()
                .contains("DATOS_INVALIDOS"));
    }

    @Test
    void rechazarMetodoNoPermitido() throws Exception {
        assertEquals(405, pedir(
                "/politicas-citacion",
                "POST",
                "ADMINISTRADOR",
                "").statusCode());
    }

    @Test
    void rechazarRutaDesconocida() throws Exception {
        assertEquals(404, pedir(
                "/politicas-citacion/otra",
                "GET",
                "ADMINISTRADOR",
                null).statusCode());
    }
}
