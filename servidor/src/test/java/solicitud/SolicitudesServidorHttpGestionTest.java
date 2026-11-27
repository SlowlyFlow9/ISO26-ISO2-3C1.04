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
import tipoprueba.RepositorioTiposPruebaMemoria;

/** CU13 - Servidor: rutas HTTP de la gestión de solicitudes (D-037). */
class SolicitudesServidorHttpGestionTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 10, 9, 0);

    private final HttpClient http = HttpClient.newHttpClient();
    private HttpServer servidor;
    private SolicitudesRepositorioMemoria solicitudes;

    @BeforeEach
    void setUp() throws Exception {
        PacientesRepositorioMemoria pacientes = new PacientesRepositorioMemoria();
        solicitudes = new SolicitudesRepositorioMemoria();
        servidor = PacientesServidorHttp.iniciar(new PacientesServiceImpl(pacientes), 0);
        SolicitudesServidorHttp.registrar(servidor,
                new SolicitudesServiceImpl(pacientes, new RepositorioTiposPruebaMemoria(), solicitudes));

        solicitudes.guardar(new SolicitudPrueba(1, 10, 7, 1, "Dolor", "Descartar",
                Prioridad.ORDINARIA, FECHA, EstadoSolicitud.PENDIENTE_CITACION));
        solicitudes.guardar(new SolicitudPrueba(2, 10, 7, 1, "Tos", "Neumonía",
                Prioridad.URGENTE, FECHA, EstadoSolicitud.CITADA));
        solicitudes.guardar(new SolicitudPrueba(3, 20, 7, 1, "Control", "Seguimiento",
                Prioridad.PREFERENTE, FECHA, EstadoSolicitud.PENDIENTE_CITACION,
                2, UnidadPeriodicidad.SEMANAS, 3, null));
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    // ---- Utilidades ----

    private HttpResponse<String> pedir(String metodo, String ruta, String cuerpo, String rol) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(
                URI.create("http://localhost:" + servidor.getAddress().getPort() + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo));
        if (rol != null) {
            peticion.header("X-Usuario", "medico").header("X-Rol", rol);
        }
        return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String cuerpo) throws Exception {
        return pedir(metodo, ruta, cuerpo, "MEDICO");
    }

    private static final String MODIFICACION =
            "{\"indicacionClinica\":\"Dolor intenso\",\"justificacion\":\"Revisión\",\"prioridad\":\"URGENTE\"}";

    // ---- Listado ----

    @Test
    @DisplayName("GET /solicitudes devuelve todas las solicitudes ordenadas por prioridad")
    void listarSinFiltros() throws Exception {
        // Ejecutar
        HttpResponse<String> r = pedir("GET", "/solicitudes", null);
        // Comprobar
        assertAll(
                () -> assertEquals(200, r.statusCode()),
                () -> assertTrue(r.body().startsWith("[{\"id\":2,")),
                () -> assertTrue(r.body().contains("\"id\":3")),
                () -> assertTrue(r.body().contains("\"intervalo\":2")));
    }

    @Test
    @DisplayName("GET /solicitudes con filtros devuelve solo las que los cumplen")
    void listarConFiltros() throws Exception {
        // Ejecutar
        HttpResponse<String> r = pedir("GET",
                "/solicitudes?pacienteId=10&estado=PENDIENTE_CITACION&prioridad=ORDINARIA&otro=x&vacio=", null);
        // Comprobar
        assertAll(
                () -> assertEquals(200, r.statusCode()),
                () -> assertTrue(r.body().contains("\"id\":1")),
                () -> assertTrue(!r.body().contains("\"id\":2") && !r.body().contains("\"id\":3")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pacienteId=abc", "pacienteId=0", "estado=PENDIENTE", "prioridad=ALTA"})
    @DisplayName("Un filtro no válido devuelve 400")
    void filtroNoValido(String consulta) throws Exception {
        // Ejecutar y comprobar
        assertEquals(400, pedir("GET", "/solicitudes?" + consulta, null).statusCode());
    }

    @Test
    @DisplayName("Los filtros se leen aunque la consulta esté vacía o mal formada")
    void leerFiltroCasosLimite() {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(FiltroSolicitudes.vacio(), SolicitudesServidorHttp.leerFiltro(null)),
                () -> assertEquals(FiltroSolicitudes.vacio(), SolicitudesServidorHttp.leerFiltro(" ")),
                () -> assertEquals(FiltroSolicitudes.vacio(), SolicitudesServidorHttp.leerFiltro("sinIgual&estado=")),
                () -> assertEquals(new FiltroSolicitudes(5L, EstadoSolicitud.ANULADA, null),
                        SolicitudesServidorHttp.leerFiltro("pacienteId=5&estado=ANULADA")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> SolicitudesServidorHttp.leerFiltro("estado=OTRO")));
    }

    // ---- Modificación ----

    @Test
    @DisplayName("PUT /solicitudes/{id} modifica la solicitud y devuelve 200")
    void modificar() throws Exception {
        // Ejecutar
        HttpResponse<String> r = pedir("PUT", "/solicitudes/1", MODIFICACION);
        // Comprobar
        assertAll(
                () -> assertEquals(200, r.statusCode()),
                () -> assertTrue(r.body().contains("\"indicacionClinica\":\"Dolor intenso\"")),
                () -> assertEquals(Prioridad.URGENTE, solicitudes.buscarPorId(1).orElseThrow().getPrioridad()));
    }

    @Test
    @DisplayName("PUT de un seguimiento cambia su periodicidad")
    void modificarSeguimiento() throws Exception {
        // Ejecutar
        HttpResponse<String> r = pedir("PUT", "/solicitudes/3",
                "{\"indicacionClinica\":\"Control\",\"justificacion\":\"Seguimiento\",\"prioridad\":\"PREFERENTE\","
                + "\"intervalo\":1,\"unidad\":\"MESES\",\"numeroRepeticiones\":6}");
        // Comprobar
        assertAll(
                () -> assertEquals(200, r.statusCode()),
                () -> assertEquals(UnidadPeriodicidad.MESES, solicitudes.buscarPorId(3).orElseThrow().getUnidad()));
    }

    @Test
    @DisplayName("PUT con datos no válidos o cuerpo vacío devuelve 400")
    void modificarDatosNoValidos() throws Exception {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(400, pedir("PUT", "/solicitudes/1",
                        "{\"indicacionClinica\":\" \",\"justificacion\":\"J\",\"prioridad\":\"URGENTE\"}").statusCode()),
                () -> assertEquals(400, pedir("PUT", "/solicitudes/1", "").statusCode()),
                () -> assertEquals(400, pedir("PUT", "/solicitudes/1", "{no es json").statusCode()),
                () -> assertEquals(400, pedir("PUT", "/solicitudes/abc", MODIFICACION).statusCode()));
    }

    @Test
    @DisplayName("PUT de una solicitud citada devuelve 409 y de una inexistente 404")
    void modificarConflictoYNoEncontrada() throws Exception {
        // Ejecutar
        HttpResponse<String> citada = pedir("PUT", "/solicitudes/2", MODIFICACION);
        HttpResponse<String> inexistente = pedir("PUT", "/solicitudes/99", MODIFICACION);
        // Comprobar
        assertAll(
                () -> assertEquals(409, citada.statusCode()),
                () -> assertEquals("{\"error\":\"ESTADO_NO_PERMITIDO\"}", citada.body()),
                () -> assertEquals(404, inexistente.statusCode()));
    }

    // ---- Anulación ----

    @Test
    @DisplayName("POST /solicitudes/{id}/anulacion anula la solicitud con su motivo")
    void anular() throws Exception {
        // Ejecutar
        HttpResponse<String> r = pedir("POST", "/solicitudes/1/anulacion", "{\"motivo\":\"Duplicada\"}");
        // Comprobar
        SolicitudPrueba anulada = solicitudes.buscarPorId(1).orElseThrow();
        assertAll(
                () -> assertEquals(200, r.statusCode()),
                () -> assertTrue(r.body().contains("\"estado\":\"ANULADA\"")),
                () -> assertTrue(r.body().contains("\"motivoAnulacion\":\"Duplicada\"")),
                () -> assertEquals(EstadoSolicitud.ANULADA, anulada.getEstado()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"motivo\":\" \"}", "{\"motivo\":{}}", "[1]", "{roto"})
    @DisplayName("Una anulación sin motivo válido devuelve 400 y no anula")
    void anularSinMotivo(String cuerpo) throws Exception {
        // Ejecutar
        HttpResponse<String> r = pedir("POST", "/solicitudes/1/anulacion", cuerpo);
        // Comprobar
        assertAll(
                () -> assertEquals(400, r.statusCode()),
                () -> assertNull(solicitudes.buscarPorId(1).orElseThrow().getMotivoAnulacion()));
    }

    @Test
    @DisplayName("Anular una solicitud citada devuelve 409 y una inexistente 404")
    void anularConflictoYNoEncontrada() throws Exception {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(409, pedir("POST", "/solicitudes/2/anulacion", "{\"motivo\":\"M\"}").statusCode()),
                () -> assertEquals(404, pedir("POST", "/solicitudes/99/anulacion", "{\"motivo\":\"M\"}").statusCode()));
    }

    @Test
    @DisplayName("Un método distinto de POST sobre la anulación devuelve 405")
    void anularConOtroMetodo() throws Exception {
        // Ejecutar y comprobar
        assertEquals(405, pedir("GET", "/solicitudes/1/anulacion", null).statusCode());
    }

    // ---- Permisos y rutas existentes ----

    @Test
    @DisplayName("Sin rol de Médico todas las operaciones de gestión devuelven 403")
    void sinPermiso() throws Exception {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(403, pedir("GET", "/solicitudes", null, "PACIENTE").statusCode()),
                () -> assertEquals(403, pedir("GET", "/solicitudes", null, null).statusCode()),
                () -> assertEquals(403, pedir("PUT", "/solicitudes/1", MODIFICACION, "ADMINISTRADOR").statusCode()),
                () -> assertEquals(403, pedir("POST", "/solicitudes/1/anulacion", "{\"motivo\":\"M\"}",
                        "PERSONAL_ADMINISTRATIVO").statusCode()),
                () -> assertEquals(EstadoSolicitud.PENDIENTE_CITACION,
                        solicitudes.buscarPorId(1).orElseThrow().getEstado()));
    }

    @Test
    @DisplayName("Las rutas de CU11 siguen funcionando: detalle por id y método no permitido")
    void rutasExistentesSiguenFuncionando() throws Exception {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals(200, pedir("GET", "/solicitudes/3", null).statusCode()),
                () -> assertEquals(405, pedir("DELETE", "/solicitudes/3", null).statusCode()),
                () -> assertEquals(405, pedir("DELETE", "/solicitudes", null).statusCode()),
                () -> assertEquals(405, pedir("PUT", "/solicitudes/tipos", MODIFICACION).statusCode()));
    }
}
