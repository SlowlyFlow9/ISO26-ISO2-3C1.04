package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Paciente;
import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;
import tipoprueba.RepositorioTiposPruebaMemoria;
import tipoprueba.TipoPrueba;

/** CU12: protocolo HTTP de la prescripción de pruebas de seguimiento (D-022 y D-030). */
class SolicitudesServidorHttpSeguimientoTest {
    private static final String PERIODICIDAD =
            ",\"intervalo\":7,\"unidad\":\"SEMANAS\",\"numeroRepeticiones\":3";

    private HttpServer servidor;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void arrancar() throws Exception {
        PacientesRepositorioMemoria pacientes = new PacientesRepositorioMemoria();
        RepositorioTiposPruebaMemoria tipos = new RepositorioTiposPruebaMemoria();
        servidor = PacientesServidorHttp.iniciar(new PacientesServiceImpl(pacientes), 0);
        SolicitudesServidorHttp.registrar(servidor,
                new SolicitudesServiceImpl(pacientes, tipos, new SolicitudesRepositorioMemoria()));

        Paciente paciente = new Paciente();
        paciente.setNombre("Paciente");
        paciente.setApellidos("De prueba");
        paciente.setDni("11111111H");
        paciente.setFechaNacimiento(LocalDate.of(1995, 1, 1));
        paciente.setTelefono("600123456");
        paciente.setActivo(true);
        pacientes.guardar(paciente);

        tipos.guardar(new TipoPrueba(0, "Radiografia", "Prueba diagnostica", 15, false, "", true));
    }

    @AfterEach
    void parar() { servidor.stop(0); }

    private String url(String ruta) { return "http://localhost:" + servidor.getAddress().getPort() + ruta; }

    private static String cuerpo(String extra) {
        return "{\"pacienteId\":1,\"medicoId\":2,\"tipoPruebaId\":1,\"indicacionClinica\":\"Dolor\","
                + "\"justificacion\":\"Control\",\"prioridad\":\"PREFERENTE\"" + extra + "}";
    }

    private HttpResponse<String> post(String json, String rol) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url("/solicitudes")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (rol != null) b.header("X-Usuario", "medico").header("X-Rol", rol);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> prescribir(String extra) throws Exception { return post(cuerpo(extra), "MEDICO"); }

    private HttpResponse<String> consultar(long id) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url("/solicitudes/" + id)))
                .header("X-Usuario", "medico").header("X-Rol", "MEDICO").GET().build();
        return http.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    // ---- Registro correcto ----

    @Test
    @DisplayName("Un seguimiento válido devuelve 201 con su periodicidad")
    void seguimientoValidoDevuelve201ConLaPeriodicidad() throws Exception {
        // Ejecutar
        HttpResponse<String> r = prescribir(PERIODICIDAD);
        // Comprobar
        assertAll(
                () -> assertEquals(201, r.statusCode()),
                () -> assertTrue(r.body().contains("\"intervalo\":7")),
                () -> assertTrue(r.body().contains("\"unidad\":\"SEMANAS\"")),
                () -> assertTrue(r.body().contains("\"numeroRepeticiones\":3")),
                () -> assertTrue(r.body().contains("\"estado\":\"PENDIENTE_CITACION\"")));
    }

    @Test
    @DisplayName("Consultar un seguimiento devuelve su periodicidad")
    void consultarDevuelveLaPeriodicidad() throws Exception {
        // Preparar
        prescribir(PERIODICIDAD);
        // Ejecutar
        HttpResponse<String> r = consultar(1);
        // Comprobar
        assertAll(
                () -> assertEquals(200, r.statusCode()),
                () -> assertTrue(r.body().contains("\"intervalo\":7")),
                () -> assertTrue(r.body().contains("\"unidad\":\"SEMANAS\"")));
    }

    @Test
    @DisplayName("Una solicitud sin periodicidad devuelve 201 y no incluye campos de seguimiento")
    void solicitudSinPeriodicidadNoIncluyeCamposDeSeguimiento() throws Exception {
        // Ejecutar
        HttpResponse<String> r = prescribir("");
        // Comprobar
        assertAll(
                () -> assertEquals(201, r.statusCode()),
                () -> assertFalse(r.body().contains("intervalo")),
                () -> assertFalse(r.body().contains("unidad")));
    }

    @Test
    @DisplayName("Un seguimiento con una prescripción original existente devuelve 201")
    void seguimientoConOrigenExistenteDevuelve201() throws Exception {
        // Preparar
        prescribir(PERIODICIDAD);
        // Ejecutar
        HttpResponse<String> r = prescribir(PERIODICIDAD + ",\"solicitudOrigenId\":1");
        // Comprobar
        assertAll(
                () -> assertEquals(201, r.statusCode()),
                () -> assertTrue(r.body().contains("\"solicitudOrigenId\":1")));
    }

    // ---- Datos inválidos ----

    @Test
    @DisplayName("Un intervalo de 0 devuelve 400")
    void intervaloCeroDevuelve400() throws Exception {
        // Ejecutar
        HttpResponse<String> r = prescribir(",\"intervalo\":0,\"unidad\":\"DIAS\",\"numeroRepeticiones\":3");
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("Un número de repeticiones de 0 devuelve 400")
    void repeticionesCeroDevuelve400() throws Exception {
        // Ejecutar
        HttpResponse<String> r = prescribir(",\"intervalo\":7,\"unidad\":\"DIAS\",\"numeroRepeticiones\":0");
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("Una periodicidad incompleta devuelve 400")
    void periodicidadIncompletaDevuelve400() throws Exception {
        // Ejecutar
        HttpResponse<String> r = prescribir(",\"intervalo\":7");
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("Una unidad desconocida devuelve 400")
    void unidadDesconocidaDevuelve400() throws Exception {
        // Ejecutar
        HttpResponse<String> r = prescribir(",\"intervalo\":7,\"unidad\":\"AÑOS\",\"numeroRepeticiones\":3");
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("Un intervalo no numérico devuelve 400")
    void intervaloNoNumericoDevuelve400() throws Exception {
        // Ejecutar
        HttpResponse<String> r = prescribir(",\"intervalo\":\"abc\",\"unidad\":\"DIAS\",\"numeroRepeticiones\":3");
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("Una prescripción original inexistente devuelve 404")
    void origenInexistenteDevuelve404() throws Exception {
        // Ejecutar
        HttpResponse<String> r = prescribir(PERIODICIDAD + ",\"solicitudOrigenId\":99");
        // Comprobar
        assertEquals(404, r.statusCode());
    }

    // ---- Permisos ----

    @Test
    @DisplayName("El rol PERSONAL_ADMINISTRATIVO no puede prescribir un seguimiento: 403")
    void personalAdministrativoDevuelve403() throws Exception {
        // Ejecutar
        HttpResponse<String> r = post(cuerpo(PERIODICIDAD), "PERSONAL_ADMINISTRATIVO");
        // Comprobar
        assertEquals(403, r.statusCode());
    }

    @Test
    @DisplayName("Un seguimiento sin usuario devuelve 403")
    void sinUsuarioDevuelve403() throws Exception {
        // Ejecutar
        HttpResponse<String> r = post(cuerpo(PERIODICIDAD), null);
        // Comprobar
        assertEquals(403, r.statusCode());
    }
}
