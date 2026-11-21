package agenda;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;
import paciente.Rol;
import paciente.Usuario;

/**
 * CU29 - Pruebas de comunicación HTTP.
 *
 * Comprueba las rutas, respuestas JSON,
 * validaciones y permisos.
 */
public class HuecosAgendaServidorHttpTest {

    private HttpServer servidor;

    private final HttpClient http =
            HttpClient.newHttpClient();

    private static final String HUECO_VALIDO =
            "{\"agendaId\":1,"
            + "\"horaInicio\":\"09:00\","
            + "\"horaFin\":\"09:30\"}";

    @BeforeEach
    void preparar() throws Exception {

        // PREPARAR: repositorios del servidor.
        AgendasRepositorioMemoria agendas =
                new AgendasRepositorioMemoria();

        HuecosAgendaRepositorioMemoria huecos =
                new HuecosAgendaRepositorioMemoria();

        HuecosAgendaService servicio =
                new HuecosAgendaServiceImpl(agendas, huecos);

        // Crear una agenda válida para las pruebas.
        AgendaMedica datos = new AgendaMedica();
        datos.setMedicoId(1L);
        datos.setFecha(LocalDate.of(2026, 11, 20));
        datos.setHoraInicio(LocalTime.of(8, 0));
        datos.setHoraFin(LocalTime.of(14, 0));

        Usuario administrativo = new Usuario(
                "Administrativo",
                Rol.PERSONAL_ADMINISTRATIVO);

        new AgendasServiceImpl(agendas)
                .crear(administrativo, datos);

        // Arrancar el servidor en un puerto libre.
        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(
                        new PacientesRepositorioMemoria()),
                0);

        // Registrar las rutas de CU29.
        HuecosAgendaServidorHttp.registrar(
                servidor, servicio);
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    // Construye la URL del servidor de pruebas.
    private String url(String ruta) {
        return "http://localhost:"
                + servidor.getAddress().getPort()
                + ruta;
    }

    // Envía una petición HTTP y devuelve su respuesta.
    private HttpResponse<String> pedir(
            String metodo,
            String ruta,
            String cuerpo,
            String rol) throws Exception {

        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(
                        URI.create(url(ruta)))
                .header("Content-Type", "application/json");

        if (rol != null) {
            peticion.header("X-Usuario", "Usuario de prueba");
            peticion.header("X-Rol", rol);
        }

        peticion.method(
                metodo,
                cuerpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo));

        return http.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    // Por defecto, el usuario es personal administrativo.
    private HttpResponse<String> pedir(
            String metodo,
            String ruta,
            String cuerpo) throws Exception {

        return pedir(metodo, ruta, cuerpo,
                "PERSONAL_ADMINISTRATIVO");
    }

    @Test
    void crearYListarHuecos() throws Exception {

        // EJECUTAR: crear un hueco por HTTP.
        HttpResponse<String> creada =
                pedir("POST", "/huecos", HUECO_VALIDO);

        // COMPROBAR: respuesta 201 y datos correctos.
        assertEquals(201, creada.statusCode());

        var json = JsonParser.parseString(creada.body())
                .getAsJsonObject();

        assertEquals(1, json.get("id").getAsLong());
        assertEquals("09:00",
                json.get("horaInicio").getAsString());

        // Consultar los huecos de la agenda.
        HttpResponse<String> consulta =
                pedir("GET", "/huecos?agendaId=1", null);

        assertEquals(200, consulta.statusCode());

        assertEquals(1,
                JsonParser.parseString(consulta.body())
                        .getAsJsonArray().size());
    }

    @Test
    void bloquearYLiberarPorHttp() throws Exception {

        pedir("POST", "/huecos", HUECO_VALIDO);

        // BLOQUEAR
        HttpResponse<String> bloqueo =
                pedir("POST", "/huecos/1/bloquear", null);

        assertEquals(200, bloqueo.statusCode());

        assertTrue(
                JsonParser.parseString(bloqueo.body())
                        .getAsJsonObject()
                        .get("bloqueado").getAsBoolean());

        // LIBERAR
        HttpResponse<String> liberacion =
                pedir("POST", "/huecos/1/liberar", null);

        assertEquals(200, liberacion.statusCode());

        assertFalse(
                JsonParser.parseString(liberacion.body())
                        .getAsJsonObject()
                        .get("bloqueado").getAsBoolean());
    }

    @Test
    void rechazarUsuarioSinPermiso() throws Exception {

        // Un médico no puede crear huecos.
        assertEquals(403,
                pedir("POST", "/huecos",
                        HUECO_VALIDO, "MEDICO")
                        .statusCode());

        // Sin usuario tampoco se permite consultar.
        assertEquals(403,
                pedir("GET", "/huecos?agendaId=1",
                        null, null)
                        .statusCode());

        // No se ha guardado ningún hueco.
        HttpResponse<String> consulta =
                pedir("GET", "/huecos?agendaId=1", null);

        assertEquals("[]", consulta.body());
    }

    @Test
    void rechazarDatosIncorrectos() throws Exception {

        // La hora de fin es anterior al inicio.
        String horarioIncorrecto =
                "{\"agendaId\":1,"
                + "\"horaInicio\":\"10:00\","
                + "\"horaFin\":\"09:00\"}";

        assertEquals(400,
                pedir("POST", "/huecos",
                        horarioIncorrecto).statusCode());

        // Faltan campos obligatorios.
        assertEquals(400,
                pedir("POST", "/huecos", "{}")
                        .statusCode());

        // JSON mal formado.
        assertEquals(400,
                pedir("POST", "/huecos", "{no es json")
                        .statusCode());
    }

    @Test
    void rechazarAgendaInexistente() throws Exception {

        String datos =
                "{\"agendaId\":999,"
                + "\"horaInicio\":\"09:00\","
                + "\"horaFin\":\"09:30\"}";

        assertEquals(404,
                pedir("POST", "/huecos", datos)
                        .statusCode());

        assertEquals(404,
                pedir("GET",
                        "/huecos?agendaId=999", null)
                        .statusCode());
    }

    @Test
    void rechazarSolapamientos() throws Exception {

        pedir("POST", "/huecos", HUECO_VALIDO);

        String solapado =
                "{\"agendaId\":1,"
                + "\"horaInicio\":\"09:15\","
                + "\"horaFin\":\"09:45\"}";

        assertEquals(400,
                pedir("POST", "/huecos", solapado)
                        .statusCode());

        // El segundo hueco no debe almacenarse.
        HttpResponse<String> consulta =
                pedir("GET", "/huecos?agendaId=1", null);

        assertEquals(1,
                JsonParser.parseString(consulta.body())
                        .getAsJsonArray().size());
    }

    @Test
    void rechazarHuecoInexistente() throws Exception {

        assertEquals(404,
                pedir("POST",
                        "/huecos/999/bloquear", null)
                        .statusCode());

        assertEquals(404,
                pedir("POST",
                        "/huecos/999/liberar", null)
                        .statusCode());
    }

    @Test
    void rechazarRutasYParametrosIncorrectos()
            throws Exception {

        // Falta agendaId.
        assertEquals(400,
                pedir("GET", "/huecos", null)
                        .statusCode());

        // Identificador no numérico.
        assertEquals(400,
                pedir("GET",
                        "/huecos?agendaId=abc", null)
                        .statusCode());

        // Método incorrecto.
        assertEquals(405,
                pedir("DELETE", "/huecos", null)
                        .statusCode());

        // Ruta inexistente.
        assertEquals(404,
                pedir("GET", "/huecos/desconocido", null)
                        .statusCode());
    }

}

