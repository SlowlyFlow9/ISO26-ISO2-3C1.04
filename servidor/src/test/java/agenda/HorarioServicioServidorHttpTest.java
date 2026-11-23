package agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpServer;

import paciente.PacientesRepositorioMemoria;
import paciente.PacientesServiceImpl;
import paciente.PacientesServidorHttp;

class HorarioServicioServidorHttpTest {

    private HttpServer servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(
                    LocalTime.class,
                    (JsonSerializer<LocalTime>)
                    (hora, tipo, contexto) ->
                            new JsonPrimitive(hora.toString()))
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>)
                    (fecha, tipo, contexto) ->
                            new JsonPrimitive(fecha.toString()))
            .create();

    @BeforeEach
    void preparar() throws Exception {
        servidor = PacientesServidorHttp.iniciar(
                new PacientesServiceImpl(
                        new PacientesRepositorioMemoria()),
                0);
        HorarioServicioRepositorioMemoria repositorio =
                new HorarioServicioRepositorioMemoria();
        HorarioServicioServidorHttp.registrar(
                servidor,
                new HorarioServicioServiceImpl(repositorio));
    }

    @AfterEach
    void cerrar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private HttpResponse<String> pedir(
            String metodo,
            String rol,
            String cuerpo) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:"
                        + servidor.getAddress().getPort()
                        + "/horarios-servicio"))
                .header("X-Usuario", "admin");
        if (rol != null) {
            peticion.header("X-Rol", rol);
        }
        if ("GET".equals(metodo)) {
            peticion.GET();
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

    private HorarioServicio horarioBase() {
        HorarioServicio horario = HorarioServicio.vacio();
        horario.getDias().get(0).getIntervalos().add(
                new IntervaloHorario(
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0)));
        return horario;
    }

    @Test
    void consultarHorarioComoAdministrador() throws Exception {
        HttpResponse<String> respuesta =
                pedir("GET", "ADMINISTRADOR", null);

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("\"dia\":\"MONDAY\""));
    }

    @Test
    void modificarHorarioComoAdministrador() throws Exception {
        HttpResponse<String> respuesta = pedir(
                "PUT",
                "ADMINISTRADOR",
                gson.toJson(horarioBase()));

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("\"modificadoPor\":\"admin\""));
        assertTrue(respuesta.body().contains("\"horaInicio\":\"08:00\""));
    }

    @Test
    void rechazarUsuarioSinPermiso() throws Exception {
        assertEquals(403,
                pedir("GET", "MEDICO", null).statusCode());
    }

    @Test
    void rechazarHorarioInvalido() throws Exception {
        HorarioServicio horario = horarioBase();
        horario.getDias().get(0).getIntervalos().get(0)
                .setHoraFin(LocalTime.of(7, 0));

        assertEquals(400, pedir(
                "PUT",
                "ADMINISTRADOR",
                gson.toJson(horario)).statusCode());
    }

    @Test
    void rechazarMetodoNoPermitido() throws Exception {
        assertEquals(405,
                pedir("POST", "ADMINISTRADOR", "").statusCode());
    }

    @Test
    void rechazarRutaDesconocida() throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:"
                        + servidor.getAddress().getPort()
                        + "/horarios-servicio/no-existe"))
                .header("X-Usuario", "admin")
                .header("X-Rol", "ADMINISTRADOR")
                .GET()
                .build();

        assertEquals(404, cliente.send(
                peticion,
                HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
