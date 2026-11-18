package paciente;

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

    private HttpResponse<String> delete(String ruta, String rol) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url(ruta))).DELETE();
        if (rol != null) b.header("X-Usuario", "ana").header("X-Rol", rol);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String ruta) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url(ruta))).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    // ---- Alta correcta ----

    @Test
    @DisplayName("El alta correcta devuelve 201 con id, fecha de alta y activo")
    void altaCorrectaDevuelve201ConIdFechaAltaYActivo() throws Exception {
        // Preparar
        String cuerpo = cuerpo("12345678Z", "T1");
        // Ejecutar
        HttpResponse<String> r = alta(cuerpo);
        // Comprobar
        assertAll(
                () -> assertEquals(201, r.statusCode()),
                () -> assertTrue(r.body().contains("\"id\":1")),
                () -> assertTrue(r.body().contains("\"fechaAlta\":\"" + LocalDate.now() + "\"")),
                () -> assertTrue(r.body().contains("\"activo\":true")));
    }

    @Test
    @DisplayName("El paciente queda disponible para otros casos de uso")
    void elPacienteQuedaDisponibleParaOtrosCasosDeUso() throws Exception {
        // Preparar
        String cuerpo = cuerpo("12345678Z", "T1");
        // Ejecutar
        alta(cuerpo);
        // Comprobar
        assertEquals("12345678Z", repositorio.buscarPorId(1L).orElseThrow().getDni());
    }

    // ---- Duplicados ----

    @Test
    @DisplayName("Un DNI repetido devuelve 409 con código DUPLICADO")
    void dniRepetidoDevuelve409() throws Exception {
        // Preparar
        alta(cuerpo("12345678Z", "T1"));
        // Ejecutar
        HttpResponse<String> r = alta(cuerpo("12345678Z", "T2"));
        // Comprobar
        assertAll(
                () -> assertEquals(409, r.statusCode()),
                () -> assertTrue(r.body().contains("DUPLICADO")));
    }

    @Test
    @DisplayName("Una tarjeta repetida devuelve 409")
    void tarjetaRepetidaDevuelve409() throws Exception {
        // Preparar
        alta(cuerpo("12345678Z", "T1"));
        // Ejecutar
        HttpResponse<String> r = alta(cuerpo("87654321X", "T1"));
        // Comprobar
        assertEquals(409, r.statusCode());
    }

    // ---- Datos inválidos ----

    @Test
    @DisplayName("Un cuerpo vacío {} devuelve 400 con la lista de errores")
    void cuerpoSinCamposDevuelve400ConLaListaDeErrores() throws Exception {
        // Preparar
        String cuerpo = "{}";
        // Ejecutar
        HttpResponse<String> r = alta(cuerpo);
        // Comprobar
        assertAll(
                () -> assertEquals(400, r.statusCode()),
                () -> assertTrue(r.body().contains("DATOS_INVALIDOS")),
                () -> assertTrue(r.body().contains("\"errores\":[")));
    }

    @Test
    @DisplayName("Un DNI inválido devuelve 400")
    void dniInvalidoDevuelve400() throws Exception {
        // Preparar
        String cuerpo = cuerpo("123", "T1");
        // Ejecutar
        HttpResponse<String> r = alta(cuerpo);
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("Un email inválido devuelve 400")
    void emailInvalidoDevuelve400() throws Exception {
        // Preparar
        String cuerpo = cuerpo("12345678Z", "T1").replace("\"telefono\"", "\"email\":\"mal\",\"telefono\"");
        // Ejecutar
        HttpResponse<String> r = alta(cuerpo);
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("Una fecha de nacimiento futura devuelve 400")
    void fechaNacimientoFuturaDevuelve400() throws Exception {
        // Preparar
        String cuerpo = cuerpo("12345678Z", "T1").replace("1990-05-01", LocalDate.now().plusDays(1).toString());
        // Ejecutar
        HttpResponse<String> r = alta(cuerpo);
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("Un cuerpo que no es JSON devuelve 400")
    void cuerpoQueNoEsJsonDevuelve400() throws Exception {
        // Preparar
        String cuerpo = "esto no es json";
        // Ejecutar
        HttpResponse<String> r = alta(cuerpo);
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    @Test
    @DisplayName("Un cuerpo vacío (cadena vacía) devuelve 400")
    void cuerpoEnBlancoDevuelve400() throws Exception {
        // Preparar
        String cuerpo = "";
        // Ejecutar
        HttpResponse<String> r = alta(cuerpo);
        // Comprobar
        assertEquals(400, r.statusCode());
    }

    // ---- Permisos ----

    @Test
    @DisplayName("El rol PACIENTE recibe 403 SIN_PERMISO y no se guarda nada")
    void pacienteRecibe403() throws Exception {
        assertAltaProhibida("PACIENTE");
    }

    @Test
    @DisplayName("El rol MEDICO recibe 403 SIN_PERMISO y no se guarda nada")
    void medicoRecibe403() throws Exception {
        assertAltaProhibida("MEDICO");
    }

    @Test
    @DisplayName("El rol ADMINISTRADOR recibe 403 SIN_PERMISO y no se guarda nada")
    void administradorRecibe403() throws Exception {
        assertAltaProhibida("ADMINISTRADOR");
    }

    private void assertAltaProhibida(String rol) throws Exception {
        // Preparar
        String cuerpo = cuerpo("12345678Z", "T1");
        // Ejecutar
        HttpResponse<String> r = post(cuerpo, rol);
        // Comprobar
        assertAll(
                () -> assertEquals(403, r.statusCode()),
                () -> assertTrue(r.body().contains("SIN_PERMISO")),
                () -> assertTrue(repositorio.buscarPorId(1L).isEmpty()));
    }

    @Test
    @DisplayName("Sin usuario la petición devuelve 403")
    void sinUsuarioDevuelve403() throws Exception {
        // Preparar
        String cuerpo = cuerpo("12345678Z", "T1");
        // Ejecutar
        HttpResponse<String> r = post(cuerpo, null);
        // Comprobar
        assertEquals(403, r.statusCode());
    }

    @Test
    @DisplayName("Con un rol desconocido la petición devuelve 403")
    void rolDesconocidoDevuelve403() throws Exception {
        // Preparar
        String cuerpo = cuerpo("12345678Z", "T1");
        // Ejecutar
        HttpResponse<String> r = post(cuerpo, "SUPERUSUARIO");
        // Comprobar
        assertEquals(403, r.statusCode());
    }

    // ---- Baja (CU24) ----

    @Test
    @DisplayName("La baja de un paciente activo devuelve 204")
    void bajaDevuelve204() throws Exception {
        // Preparar
        alta(cuerpo("12345678Z", "T1"));
        // Ejecutar
        HttpResponse<String> r = delete("/pacientes/1", "PERSONAL_ADMINISTRATIVO");
        // Comprobar
        assertEquals(204, r.statusCode());
    }

    @Test
    @DisplayName("La baja es lógica: el paciente queda inactivo y conserva sus datos")
    void bajaConservaElPaciente() throws Exception {
        // Preparar
        alta(cuerpo("12345678Z", "T1"));
        // Ejecutar
        delete("/pacientes/1", "PERSONAL_ADMINISTRATIVO");
        // Comprobar
        Paciente guardado = repositorio.buscarPorId(1L).orElseThrow();
        assertAll(
                () -> assertFalse(guardado.isActivo()),
                () -> assertEquals("12345678Z", guardado.getDni()));
    }

    @Test
    @DisplayName("Dar de baja un paciente inexistente devuelve 404")
    void bajaDePacienteInexistenteDevuelve404() throws Exception {
        // Ejecutar
        HttpResponse<String> r = delete("/pacientes/99", "PERSONAL_ADMINISTRATIVO");
        // Comprobar
        assertEquals(404, r.statusCode());
    }

    @Test
    @DisplayName("Dar de baja un paciente ya inactivo devuelve 409 PACIENTE_YA_INACTIVO")
    void bajaDePacienteYaInactivoDevuelve409() throws Exception {
        // Preparar
        alta(cuerpo("12345678Z", "T1"));
        delete("/pacientes/1", "PERSONAL_ADMINISTRATIVO");
        // Ejecutar
        HttpResponse<String> r = delete("/pacientes/1", "PERSONAL_ADMINISTRATIVO");
        // Comprobar
        assertAll(
                () -> assertEquals(409, r.statusCode()),
                () -> assertTrue(r.body().contains("PACIENTE_YA_INACTIVO")));
    }

    @Test
    @DisplayName("El rol MEDICO no puede dar de baja: 403 y el paciente sigue activo")
    void bajaDelMedicoDevuelve403() throws Exception {
        // Preparar
        alta(cuerpo("12345678Z", "T1"));
        // Ejecutar
        HttpResponse<String> r = delete("/pacientes/1", "MEDICO");
        // Comprobar
        assertAll(
                () -> assertEquals(403, r.statusCode()),
                () -> assertTrue(repositorio.buscarPorId(1L).orElseThrow().isActivo()));
    }

    @Test
    @DisplayName("Una baja sin usuario devuelve 403")
    void bajaSinUsuarioDevuelve403() throws Exception {
        // Preparar
        alta(cuerpo("12345678Z", "T1"));
        // Ejecutar
        HttpResponse<String> r = delete("/pacientes/1", null);
        // Comprobar
        assertEquals(403, r.statusCode());
    }

    // ---- Método y ruta ----

    @Test
    @DisplayName("Un GET sobre /pacientes devuelve 405")
    void getSobrePacientesDevuelve405() throws Exception {
        // Ejecutar
        HttpResponse<String> r = get("/pacientes");
        // Comprobar
        assertEquals(405, r.statusCode());
    }

    @Test
    @DisplayName("Una ruta desconocida devuelve 404")
    void rutaDesconocidaDevuelve404() throws Exception {
        // Ejecutar
        HttpResponse<String> r = get("/otra");
        // Comprobar
        assertEquals(404, r.statusCode());
    }
}
