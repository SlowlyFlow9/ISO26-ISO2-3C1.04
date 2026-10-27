package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de aceptación de los criterios de #36 a través del protocolo HTTP de D-022 y D-023. */
class PacientesServidorHttpTest {
    private HttpServer servidor;
    private PacientesRepositorioMemoria repositorio;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void arrancar() throws Exception {
        repositorio = new PacientesRepositorioMemoria();
        servidor = PacientesServidorHttp.iniciar(new PacientesServiceImpl(repositorio), 0);
    }

    @AfterEach
    void parar() { servidor.stop(0); }

    private String url(String ruta) { return "http://localhost:" + servidor.getAddress().getPort() + ruta; }

    private static String cuerpo(String dni, String tarjeta) {
        return "{\"nombre\":\"Ana\",\"apellidos\":\"Pérez\",\"dni\":\"" + dni + "\",\"fechaNacimiento\":\"1990-05-01\","
                + "\"telefono\":\"600111222\"" + (tarjeta == null ? "" : ",\"numeroTarjetaSanitaria\":\"" + tarjeta + "\"") + "}";
    }

    private HttpResponse<String> post(String cuerpo, String rol) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url("/pacientes")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo));
        if (rol != null) b.header("X-Usuario", "ana").header("X-Rol", rol);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> alta(String cuerpo) throws Exception { return post(cuerpo, "PERSONAL_ADMINISTRATIVO"); }

    @Test
    void altaCorrectaDevuelve201ConIdFechaAltaYActivo() throws Exception {
        HttpResponse<String> r = alta(cuerpo("12345678Z", "T1"));
        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"id\":1"));
        assertTrue(r.body().contains("\"fechaAlta\":\"" + LocalDate.now() + "\""));
        assertTrue(r.body().contains("\"activo\":true"));
    }

    @Test
    void elPacienteQuedaDisponibleParaOtrosCasosDeUso() throws Exception {
        alta(cuerpo("12345678Z", "T1"));
        assertEquals("12345678Z", repositorio.buscarPorId(1L).orElseThrow().getDni());
    }

    @Test
    void dniOTarjetaRepetidosDevuelven409() throws Exception {
        alta(cuerpo("12345678Z", "T1"));
        HttpResponse<String> porDni = alta(cuerpo("12345678Z", "T2"));
        assertEquals(409, porDni.statusCode());
        assertTrue(porDni.body().contains("DUPLICADO"));
        assertEquals(409, alta(cuerpo("87654321X", "T1")).statusCode());
    }

    @Test
    void datosInvalidosDevuelven400ConLaListaDeErrores() throws Exception {
        HttpResponse<String> vacios = alta("{}");
        assertEquals(400, vacios.statusCode());
        assertTrue(vacios.body().contains("DATOS_INVALIDOS"));
        assertTrue(vacios.body().contains("\"errores\":["));
        assertEquals(400, alta(cuerpo("123", "T1")).statusCode());
        assertEquals(400, alta(cuerpo("12345678Z", "T1").replace("\"telefono\"", "\"email\":\"mal\",\"telefono\"")).statusCode());
        assertEquals(400, alta(cuerpo("12345678Z", "T1").replace("1990-05-01", LocalDate.now().plusDays(1).toString())).statusCode());
    }

    @Test
    void cuerpoQueNoEsJsonDevuelve400() throws Exception {
        assertEquals(400, alta("esto no es json").statusCode());
        assertEquals(400, alta("").statusCode());
    }

    @Test
    void soloElPersonalAdministrativoPuedeDarDeAlta() throws Exception {
        for (Rol rol : Rol.values()) {
            if (rol == Rol.PERSONAL_ADMINISTRATIVO) continue;
            HttpResponse<String> r = post(cuerpo("12345678Z", "T1"), rol.name());
            assertEquals(403, r.statusCode(), rol.name());
            assertTrue(r.body().contains("SIN_PERMISO"));
        }
        assertTrue(repositorio.buscarPorId(1L).isEmpty());
    }

    @Test
    void sinUsuarioOConRolDesconocidoDevuelve403() throws Exception {
        assertEquals(403, post(cuerpo("12345678Z", "T1"), null).statusCode());
        assertEquals(403, post(cuerpo("12345678Z", "T1"), "SUPERUSUARIO").statusCode());
    }

    @Test
    void otrosMetodosYRutasDevuelven405Y404() throws Exception {
        HttpResponse<String> get = http.send(HttpRequest.newBuilder(URI.create(url("/pacientes"))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(405, get.statusCode());
        HttpResponse<String> otra = http.send(HttpRequest.newBuilder(URI.create(url("/otra"))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, otra.statusCode());
    }
}
