package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sun.net.httpserver.HttpServer;

import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;

/** CU28 - Servidor: rutas HTTP de la gestión de prioridades (D-039). */
class PrioridadesServidorHttpTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 20, 9, 0);
    private static final String CAMBIO =
            "{\"prioridad\":\"URGENTE\",\"reglaPrioridadId\":1,\"motivo\":\"Empeoramiento\"}";

    private final HttpClient http = HttpClient.newHttpClient();
    private HttpServer servidor;
    private SolicitudesRepositorioMemoria solicitudes;

    @BeforeEach
    void setUp() throws Exception {
        solicitudes = new SolicitudesRepositorioMemoria();
        ReglasPrioridadRepositorioMemoria reglas = new ReglasPrioridadRepositorioMemoria();
        servidor = PacientesServidorHttp.iniciar(new PacientesServiceImpl(new PacientesRepositorioMemoria()), 0);
        PrioridadesServidorHttp.registrar(servidor,
                new PrioridadesServiceImpl(solicitudes, reglas, new CambiosPrioridadRepositorioMemoria()));

        solicitudes.guardar(new SolicitudPrueba(1, 10, 7, 1, "Dolor", "Descartar",
                Prioridad.ORDINARIA, FECHA, EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(new SolicitudPrueba(2, 20, 7, 1, "Tos", "Neumonía",
                Prioridad.URGENTE, FECHA, EstadoSolicitud.CITADA));
        reglas.guardar(new ReglaPrioridad(1, "Oncológico", "Tratamiento activo", 1, true, "root", FECHA));
        reglas.guardar(new ReglaPrioridad(2, "Retirada", "", 2, false, "root", FECHA));
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String cuerpo, String rol) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(
                URI.create("http://localhost:" + servidor.getAddress().getPort() + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo));
        if (rol != null) {
            peticion.header("X-Usuario", "admin").header("X-Rol", rol);
        }
        return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String cuerpo) throws Exception {
        return pedir(metodo, ruta, cuerpo, "PERSONAL_ADMINISTRATIVO");
    }

    // ---- Consultas ----

    @Test
    @DisplayName("GET /prioridades devuelve solo las solicitudes pendientes, sin datos clínicos")
    void listarPendientes() throws Exception {
        // Ejecutar
        HttpResponse<String> r = pedir("GET", "/prioridades", null);
        // Comprobar
        assertAll(
                () -> assertEquals(200, r.statusCode()),
                () -> assertEquals("[{\"solicitudId\":1,\"pacienteId\":10,\"tipoPruebaId\":1,"
                        + "\"prioridad\":\"ORDINARIA\",\"fechaSolicitud\":\"2026-11-20T09:00\"}]", r.body()));
    }

    @Test
    @DisplayName("GET /prioridades con filtro devuelve solo esa prioridad")
    void listarConFiltro() throws Exception {
        assertAll(
                () -> assertEquals("[]", pedir("GET", "/prioridades?prioridad=URGENTE", null).body()),
                () -> assertTrue(pedir("GET", "/prioridades?prioridad=ORDINARIA&otro=x", null).body()
                        .contains("\"solicitudId\":1")));
    }

    @Test
    @DisplayName("GET /prioridades/reglas devuelve solo las reglas activas")
    void listarReglas() throws Exception {
        // Ejecutar
        HttpResponse<String> r = pedir("GET", "/prioridades/reglas", null);
        // Comprobar
        assertAll(
                () -> assertEquals(200, r.statusCode()),
                () -> assertEquals("[{\"id\":1,\"nombre\":\"Oncológico\",\"descripcion\":\"Tratamiento activo\","
                        + "\"orden\":1}]", r.body()));
    }

    // ---- Cambio e historial ----

    @Test
    @DisplayName("PUT /prioridades/{id} cambia la prioridad y el historial lo muestra")
    void cambiarYConsultarHistorial() throws Exception {
        // Ejecutar
        HttpResponse<String> cambio = pedir("PUT", "/prioridades/1", CAMBIO);
        HttpResponse<String> historial = pedir("GET", "/prioridades/1/historial", null);
        // Comprobar
        assertAll(
                () -> assertEquals(200, cambio.statusCode()),
                () -> assertTrue(cambio.body().contains("\"prioridad\":\"URGENTE\"")),
                () -> assertEquals(Prioridad.URGENTE, solicitudes.buscarPorId(1).orElseThrow().getPrioridad()),
                () -> assertEquals(200, historial.statusCode()),
                () -> assertTrue(historial.body().contains("\"prioridadAnterior\":\"ORDINARIA\"")),
                () -> assertTrue(historial.body().contains("\"realizadoPor\":\"admin\"")),
                () -> assertTrue(historial.body().contains("\"motivo\":\"Empeoramiento\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"reglaPrioridadId\":1,\"motivo\":\"M\"}",
        "{\"prioridad\":\"URGENTE\",\"motivo\":\"M\"}",
        "{\"prioridad\":\"URGENTE\",\"reglaPrioridadId\":1,\"motivo\":\" \"}",
        "{\"prioridad\":\"ALTA\",\"reglaPrioridadId\":1,\"motivo\":\"M\"}",
        "{\"prioridad\":\"ORDINARIA\",\"reglaPrioridadId\":1,\"motivo\":\"M\"}",
        "{\"prioridad\":\"URGENTE\",\"reglaPrioridadId\":2,\"motivo\":\"M\"}",
        "",
        "{roto"})
    @DisplayName("Un cambio con datos no válidos devuelve 400 y no cambia la solicitud")
    void cambioNoValido(String cuerpo) throws Exception {
        // Ejecutar
        HttpResponse<String> r = pedir("PUT", "/prioridades/1", cuerpo);
        // Comprobar
        assertAll(
                () -> assertEquals(400, r.statusCode()),
                () -> assertEquals("{\"error\":\"DATOS_INVALIDOS\"}", r.body()),
                () -> assertEquals(Prioridad.ORDINARIA, solicitudes.buscarPorId(1).orElseThrow().getPrioridad()));
    }

    @Test
    @DisplayName("Una solicitud citada devuelve 409 y una inexistente o una regla inexistente 404")
    void conflictoYNoEncontrado() throws Exception {
        assertAll(
                () -> assertEquals(409, pedir("PUT", "/prioridades/2", CAMBIO).statusCode()),
                () -> assertEquals(404, pedir("PUT", "/prioridades/99", CAMBIO).statusCode()),
                () -> assertEquals(404, pedir("PUT", "/prioridades/1",
                        "{\"prioridad\":\"URGENTE\",\"reglaPrioridadId\":99,\"motivo\":\"M\"}").statusCode()),
                () -> assertEquals(404, pedir("GET", "/prioridades/99/historial", null).statusCode()));
    }

    // ---- Rutas, métodos y permisos ----

    @Test
    @DisplayName("Un filtro, un id o una ruta no válidos devuelven 400 o 404")
    void rutasNoValidas() throws Exception {
        assertAll(
                () -> assertEquals(400, pedir("GET", "/prioridades?prioridad=ALTA", null).statusCode()),
                () -> assertEquals(400, pedir("PUT", "/prioridades/abc", CAMBIO).statusCode()),
                () -> assertEquals(400, pedir("PUT", "/prioridades/0", CAMBIO).statusCode()),
                () -> assertEquals(400, pedir("GET", "/prioridades/abc/historial", null).statusCode()),
                () -> assertEquals(404, pedir("GET", "/prioridadesx", null).statusCode()));
    }

    @Test
    @DisplayName("Un método no permitido devuelve 405")
    void metodosNoPermitidos() throws Exception {
        assertAll(
                () -> assertEquals(405, pedir("POST", "/prioridades", CAMBIO).statusCode()),
                () -> assertEquals(405, pedir("DELETE", "/prioridades/reglas", null).statusCode()),
                () -> assertEquals(405, pedir("PUT", "/prioridades/1/historial", CAMBIO).statusCode()),
                () -> assertEquals(405, pedir("GET", "/prioridades/1", null).statusCode()));
    }

    @Test
    @DisplayName("Sin rol de personal administrativo todas las rutas devuelven 403")
    void sinPermiso() throws Exception {
        assertAll(
                () -> assertEquals(403, pedir("GET", "/prioridades", null, "MEDICO").statusCode()),
                () -> assertEquals(403, pedir("GET", "/prioridades/reglas", null, null).statusCode()),
                () -> assertEquals(403, pedir("PUT", "/prioridades/1", CAMBIO, "ADMINISTRADOR").statusCode()),
                () -> assertEquals(403, pedir("GET", "/prioridades/1/historial", null, "OTRO").statusCode()),
                () -> assertEquals(Prioridad.ORDINARIA, solicitudes.buscarPorId(1).orElseThrow().getPrioridad()));
    }

    @Test
    @DisplayName("El filtro de prioridad se lee aunque la consulta esté vacía o incompleta")
    void leerPrioridad() {
        assertAll(
                () -> assertNull(PrioridadesServidorHttp.leerPrioridad(null)),
                () -> assertNull(PrioridadesServidorHttp.leerPrioridad(" ")),
                () -> assertNull(PrioridadesServidorHttp.leerPrioridad("sinIgual&prioridad=")),
                () -> assertEquals(Prioridad.PREFERENTE, PrioridadesServidorHttp.leerPrioridad("prioridad=PREFERENTE")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> PrioridadesServidorHttp.registrar(null, null)));
    }
}
