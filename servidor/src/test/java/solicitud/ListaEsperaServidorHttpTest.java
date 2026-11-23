package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;

class ListaEsperaServidorHttpTest {

    private final HttpClient http = HttpClient.newHttpClient();
    private HttpServer servidor;

    @BeforeEach
    void setUp() throws Exception {
        SolicitudesRepositorioMemoria solicitudes =
                new SolicitudesRepositorioMemoria();
        solicitudes.guardar(new SolicitudPrueba(
                1, 10, 7, 3, "Indicacion privada", "Justificacion privada",
                Prioridad.URGENTE, LocalDateTime.of(2026, 11, 10, 9, 0),
                EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(new SolicitudPrueba(
                2, 20, 7, 4, "No debe aparecer", "No debe aparecer",
                Prioridad.PREFERENTE, LocalDateTime.of(2026, 11, 10, 9, 0),
                EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(new SolicitudPrueba(
                3, 30, 7, 5, "Tampoco debe aparecer", "Tampoco debe aparecer",
                Prioridad.URGENTE, LocalDateTime.of(2026, 11, 10, 9, 0),
                EstadoSolicitud.CITADA));

        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(new PacientesRepositorioMemoria()), 0);
        SolicitudesServidorHttp.registrarListaEspera(
                servidor, new ListaEsperaServiceImpl(solicitudes));
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    private HttpResponse<String> pedir(
            String metodo,
            String ruta,
            String rol) throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder(
                URI.create("http://localhost:"
                        + servidor.getAddress().getPort() + ruta))
                .method(metodo, HttpRequest.BodyPublishers.noBody());

        if (rol != null) {
            peticion.header("X-Usuario", "usuario")
                    .header("X-Rol", rol);
        }

        return http.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("GET /listas-espera devuelve solicitudes pendientes sin datos clínicos")
    void consultaListaSinDatosClinicos() throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta =
                pedir("GET", "/listas-espera", "PERSONAL_ADMINISTRATIVO");

        // Comprobar
        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().startsWith("[{\"solicitudId\":1")),
                () -> assertTrue(respuesta.body().contains("\"tipoPruebaId\":3")),
                () -> assertTrue(respuesta.body().contains("\"fechaSolicitud\":\"2026-11-10T09:00\"")),
                () -> assertFalse(respuesta.body().contains("Indicacion privada")),
                () -> assertFalse(respuesta.body().contains("Justificacion privada")),
                () -> assertFalse(respuesta.body().contains("\"solicitudId\":3")));
    }

    @Test
    @DisplayName("GET /listas-espera filtra por prioridad")
    void filtraPorPrioridad() throws Exception {
        // Ejecutar
        HttpResponse<String> respuesta = pedir(
                "GET",
                "/listas-espera?prioridad=PREFERENTE",
                "PERSONAL_ADMINISTRATIVO");

        // Comprobar
        assertAll(
                () -> assertEquals(200, respuesta.statusCode()),
                () -> assertTrue(respuesta.body().contains("\"solicitudId\":2")),
                () -> assertFalse(respuesta.body().contains("\"solicitudId\":1")));
    }

    @Test
    @DisplayName("La lista requiere el rol administrativo y rechaza prioridades inválidas")
    void validaPermisosYParametros() throws Exception {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(403,
                        pedir("GET", "/listas-espera", "MEDICO").statusCode()),
                () -> assertEquals(403,
                        pedir("GET", "/listas-espera", null).statusCode()),
                () -> assertEquals(400,
                        pedir("GET", "/listas-espera?prioridad=ALTA",
                                "PERSONAL_ADMINISTRATIVO").statusCode()),
                () -> assertEquals(405,
                        pedir("POST", "/listas-espera",
                                "PERSONAL_ADMINISTRATIVO").statusCode()));
    }
}
