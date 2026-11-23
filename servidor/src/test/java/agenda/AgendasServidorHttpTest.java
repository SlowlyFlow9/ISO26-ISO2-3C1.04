package agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class AgendasServidorHttpTest {

    private static final HttpClient HTTP =
            HttpClient.newHttpClient();

    private HttpServer servidor;
    private AgendasServiceImpl servicio;

    @BeforeEach
    void preparar() throws Exception {
        AgendasRepositorioMemoria repositorio =
                new AgendasRepositorioMemoria();
        servicio = new AgendasServiceImpl(repositorio);

        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(
                        new PacientesRepositorioMemoria()),
                0);

        AgendasServidorHttp.registrar(servidor, servicio);
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private String url(String ruta) {
        return "http://localhost:"
                + servidor.getAddress().getPort()
                + ruta;
    }

    private HttpResponse<String> pedir(
            String metodo,
            String ruta,
            String cuerpo,
            String rol,
            String medicoId) throws Exception {

        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(URI.create(url(ruta)))
                        .header("Content-Type", "application/json");

        if (rol != null) {
            peticion.header("X-Usuario", "medico");
            peticion.header("X-Rol", rol);
        }
        if (medicoId != null) {
            peticion.header("X-Medico-Id", medicoId);
        }

        peticion.method(
                metodo,
                cuerpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo));

        return HTTP.send(
                peticion.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> pedirComoMedico(
            String metodo,
            String ruta,
            String cuerpo) throws Exception {

        return pedir(metodo, ruta, cuerpo, "MEDICO", "1");
    }

    private AgendaMedica agenda(long medicoId) throws Exception {
        AgendaMedica datos = new AgendaMedica();
        datos.setMedicoId(medicoId);
        datos.setFecha(LocalDate.of(2026, 11, 17));
        datos.setHoraInicio(LocalTime.of(8, 0));
        datos.setHoraFin(LocalTime.of(15, 0));
        return servicio.crear(
                new Usuario("personal", Rol.PERSONAL_ADMINISTRATIVO),
                datos);
    }

    @Test
    void medicoPuedeCrearListarModificarYEliminarAgendaPropia()
            throws Exception {

        agenda(2L);

        String datos = """
                {
                  "fecha": "2026-11-18",
                  "horaInicio": "09:00",
                  "horaFin": "16:00",
                  "observaciones": "Agenda propia",
                  "medicoId": 2
                }
                """;

        HttpResponse<String> creada =
                pedirComoMedico(
                        "POST", "/agendas-realizacion", datos);

        assertEquals(201, creada.statusCode());
        var agendaCreada = JsonParser.parseString(creada.body())
                .getAsJsonObject();
        assertEquals(1L, agendaCreada.get("medicoId").getAsLong());
        assertEquals("2026-11-18",
                agendaCreada.get("fecha").getAsString());
        assertEquals("09:00",
                agendaCreada.get("horaInicio").getAsString());

        HttpResponse<String> listado =
                pedirComoMedico(
                        "GET", "/agendas-realizacion", null);
        assertEquals(200, listado.statusCode());
        assertEquals(1,
                JsonParser.parseString(listado.body())
                        .getAsJsonArray().size());

        String cambios = """
                {
                  "medicoId": 2,
                  "fecha": "2026-11-18",
                  "horaInicio": "10:00",
                  "horaFin": "17:00"
                }
                """;
        HttpResponse<String> modificada =
                pedirComoMedico(
                        "PUT",
                        "/agendas-realizacion/2",
                        cambios);

        assertEquals(200, modificada.statusCode());
        var agendaModificada = JsonParser.parseString(modificada.body())
                .getAsJsonObject();
        assertEquals(1L,
                agendaModificada.get("medicoId").getAsLong());
        assertEquals("10:00",
                agendaModificada.get("horaInicio").getAsString());

        assertEquals(204,
                pedirComoMedico(
                        "DELETE",
                        "/agendas-realizacion/2",
                        null)
                        .statusCode());

        HttpResponse<String> consulta =
                pedirComoMedico(
                        "GET", "/agendas-realizacion/2", null);
        assertEquals(200, consulta.statusCode());
        assertFalse(
                JsonParser.parseString(consulta.body())
                        .getAsJsonObject()
                        .get("activo").getAsBoolean());
    }

    @Test
    void medicoNoPuedeConsultarModificarNiEliminarAgendaAjena()
            throws Exception {

        AgendaMedica ajena = agenda(2L);

        assertEquals(403,
                pedirComoMedico(
                        "GET",
                        "/agendas-realizacion/" + ajena.getId(),
                        null)
                        .statusCode());

        assertEquals(403,
                pedirComoMedico(
                        "PUT",
                        "/agendas-realizacion/" + ajena.getId(),
                        "{}")
                        .statusCode());

        assertEquals(403,
                pedirComoMedico(
                        "DELETE",
                        "/agendas-realizacion/" + ajena.getId(),
                        null)
                        .statusCode());

        assertTrue(servicio.consultar(
                new Usuario("personal", Rol.PERSONAL_ADMINISTRATIVO),
                ajena.getId()).isActivo());
    }

    @Test
    void requiereRolMedicoEIdentificador()
            throws Exception {

        assertEquals(403,
                pedir(
                        "GET",
                        "/agendas-realizacion",
                        null,
                        "MEDICO",
                        null)
                        .statusCode());

        assertEquals(403,
                pedir(
                        "GET",
                        "/agendas-realizacion",
                        null,
                        "PERSONAL_ADMINISTRATIVO",
                        "1")
                        .statusCode());
    }

    @Test
    void rechazaDatosRutasYMetodosInvalidos()
            throws Exception {

        assertEquals(400,
                pedirComoMedico(
                        "POST",
                        "/agendas-realizacion",
                        "{no es json")
                        .statusCode());

        assertEquals(400,
                pedirComoMedico(
                        "GET",
                        "/agendas-realizacion/no-numero",
                        null)
                        .statusCode());

        assertEquals(405,
                pedirComoMedico(
                        "DELETE",
                        "/agendas-realizacion",
                        null)
                        .statusCode());

        assertEquals(404,
                pedirComoMedico(
                        "GET",
                        "/ruta-inexistente",
                        null)
                        .statusCode());
    }
}
