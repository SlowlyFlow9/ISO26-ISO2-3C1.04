package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import agenda.AgendaMedica;
import agenda.AgendasRepositorioMemoria;
import agenda.HuecoAgenda;
import agenda.HuecosAgendaRepositorioMemoria;
import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;
import solicitud.SolicitudesRepositorioMemoria;

/** CU03: rutas HTTP de huecos libres y cambio de cita del paciente. */
class CitasServidorHttpCambioTest {

    private HttpServer servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();

    private CitasRepositorioMemoria repositorio;

    private HuecoAgenda actual;
    private HuecoAgenda libre;
    private HuecoAgenda bloqueado;

    private long citaPropia;
    private long citaAjena;
    private long citaCercana;
    private long citaCancelada;

    @BeforeEach
    void preparar() throws Exception {

        repositorio = new CitasRepositorioMemoria();
        AgendasRepositorioMemoria agendas =
                new AgendasRepositorioMemoria();
        HuecosAgendaRepositorioMemoria huecos =
                new HuecosAgendaRepositorioMemoria();

        IdentidadPacienteMemoria identidad =
                new IdentidadPacienteMemoria();
        identidad.vincular("paciente1", 10L);
        identidad.vincular("paciente2", 20L);

        AgendaMedica agenda = new AgendaMedica();
        agenda.setMedicoId(7L);
        agenda.setFecha(LocalDate.now().plusDays(10));
        agenda.setHoraInicio(LocalTime.of(8, 0));
        agenda.setHoraFin(LocalTime.of(14, 0));
        agenda.setActivo(true);
        long agendaId = agendas.guardar(agenda).getId();

        actual = crearHueco(huecos, agendaId, 9, false);
        libre = crearHueco(huecos, agendaId, 10, false);
        bloqueado = crearHueco(huecos, agendaId, 11, true);

        LocalDateTime enDiezDias = LocalDateTime.of(
                agenda.getFecha(), LocalTime.of(9, 0));

        citaPropia = repositorio.guardar(crearCita(
                10L, enDiezDias, EstadoCita.PROGRAMADA,
                actual.getId())).getId();
        citaAjena = repositorio.guardar(crearCita(
                20L, enDiezDias, EstadoCita.PROGRAMADA,
                null)).getId();
        citaCercana = repositorio.guardar(crearCita(
                10L, LocalDateTime.now().plusHours(2),
                EstadoCita.PROGRAMADA, null)).getId();
        citaCancelada = repositorio.guardar(crearCita(
                10L, enDiezDias, EstadoCita.CANCELADA,
                null)).getId();

        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(
                        new PacientesRepositorioMemoria()),
                0);

        CitasServidorHttp.registrar(
                servidor,
                new CitasServiceImpl(
                        repositorio,
                        identidad,
                        new CitasAdministracionServiceImpl(
                                repositorio,
                                new SolicitudesRepositorioMemoria(),
                                new PacientesRepositorioMemoria(),
                                agendas,
                                huecos),
                        new PoliticaCitaRepositorioMemoria()));
    }

    @AfterEach
    void cerrar() {

        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private HuecoAgenda crearHueco(
            HuecosAgendaRepositorioMemoria huecos,
            long agendaId, int hora, boolean bloqueado) {

        HuecoAgenda hueco = new HuecoAgenda();
        hueco.setAgendaId(agendaId);
        hueco.setHoraInicio(LocalTime.of(hora, 0));
        hueco.setHoraFin(LocalTime.of(hora, 30));
        hueco.setBloqueado(bloqueado);

        return huecos.guardar(hueco);
    }

    private Cita crearCita(
            long pacienteId,
            LocalDateTime fecha,
            EstadoCita estado,
            Long huecoId) {

        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setMedicoId(7L);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(fecha);
        cita.setEstado(estado);
        cita.setHuecoId(huecoId);

        return cita;
    }

    private HttpResponse<String> pedir(
            String ruta,
            String metodo,
            String cuerpo,
            String usuario,
            String rol) throws Exception {

        HttpRequest.Builder peticion = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:"
                        + servidor.getAddress().getPort() + ruta));

        if (usuario != null) {
            peticion.header("X-Usuario", usuario);
        }

        if (rol != null) {
            peticion.header("X-Rol", rol);
        }

        peticion.method(metodo, cuerpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(cuerpo));

        return cliente.send(peticion.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> comoPaciente(
            String ruta, String metodo, String cuerpo)
            throws Exception {

        return pedir(ruta, metodo, cuerpo, "paciente1", "PACIENTE");
    }

    private HttpResponse<String> cambiar(long citaId, String cuerpo)
            throws Exception {

        return comoPaciente(
                "/citas/" + citaId + "/cambiar", "POST", cuerpo);
    }

    private String hueco(long id) {
        return "{\"huecoId\":" + id + "}";
    }

    // ---------- huecos libres ----------

    @Test
    void listarHuecosLibres() throws Exception {

        var respuesta = comoPaciente(
                "/citas/huecos-libres", "GET", null);

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body()
                .contains("\"id\":" + libre.getId()));
        assertFalse(respuesta.body()
                .contains("\"id\":" + actual.getId() + ","));
        assertFalse(respuesta.body()
                .contains("\"id\":" + bloqueado.getId() + ","));
        assertTrue(respuesta.body().contains("\"medicoId\":7"));
        assertTrue(respuesta.body().contains("\"fechaHora\":\""));
    }

    @Test
    void listarHuecosRechazaMedico() throws Exception {

        var respuesta = pedir("/citas/huecos-libres", "GET",
                null, "medico1", "MEDICO");

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void listarHuecosRechazaUsuarioSinCabeceras() throws Exception {

        var respuesta = pedir("/citas/huecos-libres", "GET",
                null, null, null);

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void listarHuecosRechazaMetodoIncorrecto() throws Exception {

        var respuesta = comoPaciente(
                "/citas/huecos-libres", "POST", null);

        assertEquals(405, respuesta.statusCode());
    }

    // ---------- cambio de cita ----------

    @Test
    void cambiarCitaCorrectamente() throws Exception {

        var respuesta = cambiar(citaPropia, hueco(libre.getId()));

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains(
                "\"huecoId\":" + libre.getId()));
        assertTrue(respuesta.body()
                .contains("\"estado\":\"PROGRAMADA\""));
        assertEquals(libre.getId(),
                repositorio.buscarPorId(citaPropia)
                        .orElseThrow().getHuecoId());
    }

    @Test
    void rechazarHuecoBloqueado() throws Exception {

        var respuesta = cambiar(citaPropia, hueco(bloqueado.getId()));

        assertEquals(409, respuesta.statusCode());
        assertTrue(respuesta.body().contains("HUECO_NO_DISPONIBLE"));
    }

    @Test
    void rechazarHuecoInexistente() throws Exception {

        var respuesta = cambiar(citaPropia, hueco(999));

        assertEquals(404, respuesta.statusCode());
        assertTrue(respuesta.body().contains("HUECO_NO_ENCONTRADO"));
    }

    @Test
    void rechazarCitaCancelada() throws Exception {

        var respuesta = cambiar(citaCancelada, hueco(libre.getId()));

        assertEquals(409, respuesta.statusCode());
        assertTrue(respuesta.body().contains("NO_CAMBIABLE"));
    }

    @Test
    void rechazarCambioFueraDePolitica() throws Exception {

        var respuesta = cambiar(citaCercana, hueco(libre.getId()));

        assertEquals(409, respuesta.statusCode());
        assertTrue(respuesta.body().contains("FUERA_DE_POLITICA"));
    }

    @Test
    void rechazarCitaDeOtroPaciente() throws Exception {

        var respuesta = cambiar(citaAjena, hueco(libre.getId()));

        assertEquals(404, respuesta.statusCode());
    }

    @Test
    void rechazarCitaInexistente() throws Exception {

        var respuesta = cambiar(999, hueco(libre.getId()));

        assertEquals(404, respuesta.statusCode());
    }

    @Test
    void rechazarMedico() throws Exception {

        var respuesta = pedir("/citas/" + citaPropia + "/cambiar",
                "POST", hueco(libre.getId()), "medico1", "MEDICO");

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarUsuarioSinCabeceras() throws Exception {

        var respuesta = pedir("/citas/" + citaPropia + "/cambiar",
                "POST", hueco(libre.getId()), null, null);

        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void rechazarIdentificadorDeCitaInvalido() throws Exception {

        var respuesta = comoPaciente(
                "/citas/abc/cambiar", "POST", hueco(libre.getId()));

        assertEquals(400, respuesta.statusCode());
    }

    @Test
    void rechazarCuerpoVacio() throws Exception {

        assertEquals(400, cambiar(citaPropia, null).statusCode());
    }

    @Test
    void rechazarCuerpoQueNoEsJson() throws Exception {

        assertEquals(400, cambiar(citaPropia, "hueco 3").statusCode());
    }

    @Test
    void rechazarCuerpoSinHuecoId() throws Exception {

        assertEquals(400,
                cambiar(citaPropia, "{\"otro\":3}").statusCode());
    }

    @Test
    void rechazarHuecoIdNoNumerico() throws Exception {

        assertEquals(400,
                cambiar(citaPropia, "{\"huecoId\":\"x\"}").statusCode());
    }

    @Test
    void rechazarHuecoIdNoPositivo() throws Exception {

        assertEquals(400, cambiar(citaPropia, hueco(0)).statusCode());
    }

    @Test
    void rechazarMetodoIncorrecto() throws Exception {

        var respuesta = comoPaciente(
                "/citas/" + citaPropia + "/cambiar", "GET", null);

        assertEquals(405, respuesta.statusCode());
    }
}
