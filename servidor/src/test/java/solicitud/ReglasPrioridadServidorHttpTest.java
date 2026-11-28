package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;

class ReglasPrioridadServidorHttpTest {

    private HttpServer servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @BeforeEach
    void preparar() throws Exception {
        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(
                        new PacientesRepositorioMemoria()),
                0);
        ReglasPrioridadServidorHttp.registrar(
                servidor,
                new ReglasPrioridadServiceImpl(
                        new ReglasPrioridadRepositorioMemoria()));
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
                        + servidor.getAddress().getPort() + ruta))
                .header("X-Usuario", "admin");
        if (rol != null) {
            peticion.header("X-Rol", rol);
        }
        if (metodo.equals("GET")) {
            peticion.GET();
        } else if (metodo.equals("DELETE")) {
            peticion.DELETE();
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

    private String datos(String nombre, String descripcion, int orden) {
        return gson.toJson(new DatosReglaPrioridad(
                nombre, descripcion, orden));
    }

    @Test
    void crearListarConsultarModificarYDarDeBaja()
            throws Exception {
        HttpResponse<String> creada = pedir(
                "/reglas-prioridad", "POST", "ADMINISTRADOR",
                datos("Urgente", "Alta prioridad", 1));

        assertEquals(201, creada.statusCode());
        assertEquals("/reglas-prioridad/1",
                creada.headers().firstValue("Location").orElseThrow());
        assertTrue(creada.body().contains("\"activo\":true"));

        assertEquals(200, pedir(
                "/reglas-prioridad", "GET", "ADMINISTRADOR", null)
                .statusCode());
        assertEquals(200, pedir(
                "/reglas-prioridad/1", "GET", "ADMINISTRADOR", null)
                .statusCode());

        HttpResponse<String> modificada = pedir(
                "/reglas-prioridad/1", "PUT", "ADMINISTRADOR",
                datos("Preferente", "Media prioridad", 2));
        assertEquals(200, modificada.statusCode());
        assertTrue(modificada.body().contains("\"nombre\":\"Preferente\""));

        assertEquals(204, pedir(
                "/reglas-prioridad/1", "DELETE", "ADMINISTRADOR", null)
                .statusCode());
        HttpResponse<String> consultada = pedir(
                "/reglas-prioridad/1", "GET", "ADMINISTRADOR", null);
        assertEquals(200, consultada.statusCode());
        assertTrue(consultada.body().contains("\"activo\":false"));
    }

    @Test
    void devuelveErroresParaPermisosDatosYConflictos()
            throws Exception {
        assertEquals(403, pedir(
                "/reglas-prioridad", "GET", "MEDICO", null)
                .statusCode());
        assertEquals(403, pedir(
                "/reglas-prioridad", "GET", null, null)
                .statusCode());
        assertEquals(400, pedir(
                "/reglas-prioridad", "POST", "ADMINISTRADOR",
                "{invalido").statusCode());
        assertEquals(400, pedir(
                "/reglas-prioridad", "POST", "ADMINISTRADOR",
                datos(" ", "", 1)).statusCode());

        pedir("/reglas-prioridad", "POST", "ADMINISTRADOR",
                datos("Urgente", "", 1));
        assertEquals(409, pedir(
                "/reglas-prioridad", "POST", "ADMINISTRADOR",
                datos("Otra", "", 1)).statusCode());
        assertEquals(404, pedir(
                "/reglas-prioridad/999", "GET", "ADMINISTRADOR", null)
                .statusCode());
        assertEquals(400, pedir(
                "/reglas-prioridad/abc", "GET", "ADMINISTRADOR", null)
                .statusCode());
    }

    @Test
    void devuelveNoEncontradoYMetodoNoPermitido()
            throws Exception {
        assertEquals(404, pedir(
                "/reglas-prioridad-otra", "GET", "ADMINISTRADOR", null)
                .statusCode());
        assertEquals(405, pedir(
                "/reglas-prioridad", "PUT", "ADMINISTRADOR", "{}")
                .statusCode());
        assertEquals(405, pedir(
                "/reglas-prioridad/1", "POST", "ADMINISTRADOR", "{}")
                .statusCode());
    }
}
