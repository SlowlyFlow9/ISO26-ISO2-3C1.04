package agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.Usuario;

class AgendasServiceHttpTest {

    private static final Usuario MEDICO =
            new Usuario("Juan", Rol.MEDICO, 7L);

    private HttpServer servidor;
    private AgendasServiceHttp servicio;
    private volatile int codigoRespuesta;
    private volatile String jsonRespuesta;
    private volatile String metodoRecibido;
    private volatile String rutaRecibida;
    private volatile String cuerpoRecibido;
    private volatile String usuarioRecibido;
    private volatile String rolRecibido;
    private volatile String medicoIdRecibido;

    @BeforeEach
    void preparar() throws IOException {
        codigoRespuesta = 200;
        jsonRespuesta = "{}";

        servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(
                "/agendas-realizacion",
                exchange -> {
                    metodoRecibido = exchange.getRequestMethod();
                    rutaRecibida = exchange.getRequestURI().toString();
                    cuerpoRecibido = new String(
                            exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8);
                    usuarioRecibido = exchange.getRequestHeaders()
                            .getFirst("X-Usuario");
                    rolRecibido = exchange.getRequestHeaders()
                            .getFirst("X-Rol");
                    medicoIdRecibido = exchange.getRequestHeaders()
                            .getFirst("X-Medico-Id");

                    byte[] bytes = jsonRespuesta.getBytes(
                            StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set(
                            "Content-Type", "application/json");
                    exchange.sendResponseHeaders(
                            codigoRespuesta,
                            codigoRespuesta == 204 ? -1 : bytes.length);
                    if (codigoRespuesta != 204) {
                        try (var salida = exchange.getResponseBody()) {
                            salida.write(bytes);
                        }
                    } else {
                        exchange.close();
                    }
                });
        servidor.start();

        servicio = new AgendasServiceHttp(
                "http://127.0.0.1:"
                        + servidor.getAddress().getPort());
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private String agendaJson() {
        return """
                {
                  "id": 4,
                  "medicoId": 7,
                  "fecha": "2026-11-17",
                  "horaInicio": "08:00",
                  "horaFin": "15:00",
                  "observaciones": "Agenda",
                  "activo": true
                }
                """;
    }

    private AgendaMedica datos() {
        AgendaMedica agenda = new AgendaMedica();
        agenda.setMedicoId(7L);
        agenda.setFecha(LocalDate.of(2026, 11, 17));
        agenda.setHoraInicio(LocalTime.of(8, 0));
        agenda.setHoraFin(LocalTime.of(15, 0));
        agenda.setObservaciones("Agenda");
        return agenda;
    }

    @Test
    void crearEnvíaIdentidadCompletaYDeserializaAgenda() {
        codigoRespuesta = 201;
        jsonRespuesta = agendaJson();

        AgendaMedica creada =
                servicio.crearPropia(MEDICO, datos());

        assertEquals("POST", metodoRecibido);
        assertEquals("/agendas-realizacion", rutaRecibida);
        assertEquals("Juan", usuarioRecibido);
        assertEquals("MEDICO", rolRecibido);
        assertEquals("7", medicoIdRecibido);
        assertTrue(cuerpoRecibido.contains("\"fecha\":\"2026-11-17\""));
        assertEquals(LocalDate.of(2026, 11, 17), creada.getFecha());
        assertEquals(LocalTime.of(8, 0), creada.getHoraInicio());
    }

    @Test
    void listarDeserializaAgendasPropias() {
        jsonRespuesta = "[" + agendaJson() + "]";

        List<AgendaMedica> resultado =
                servicio.listarPropias(MEDICO);

        assertEquals("GET", metodoRecibido);
        assertEquals("/agendas-realizacion", rutaRecibida);
        assertEquals(1, resultado.size());
        assertEquals(4L, resultado.get(0).getId());
    }

    @Test
    void consultarModificarYEliminarUsanRutasYMetodosCorrectos() {
        jsonRespuesta = agendaJson();

        assertEquals(4L,
                servicio.consultarPropia(MEDICO, 4L).getId());
        assertEquals("GET", metodoRecibido);
        assertEquals("/agendas-realizacion/4", rutaRecibida);

        assertEquals(4L,
                servicio.modificarPropia(
                        MEDICO, 4L, datos()).getId());
        assertEquals("PUT", metodoRecibido);
        assertEquals("/agendas-realizacion/4", rutaRecibida);

        codigoRespuesta = 204;
        servicio.eliminarPropia(MEDICO, 4L);
        assertEquals("DELETE", metodoRecibido);
        assertEquals("/agendas-realizacion/4", rutaRecibida);
    }

    @Test
    void traduceErroresHttp() {
        codigoRespuesta = 400;
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearPropia(MEDICO, datos()));

        codigoRespuesta = 403;
        assertThrows(SecurityException.class,
                () -> servicio.listarPropias(MEDICO));

        codigoRespuesta = 404;
        assertThrows(NoSuchElementException.class,
                () -> servicio.consultarPropia(MEDICO, 4L));

        codigoRespuesta = 500;
        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.listarPropias(MEDICO));
    }

    @Test
    void rechazaIdentidadDatosEIdsInvalidos() {
        Usuario sinId = new Usuario("Juan", Rol.MEDICO);
        assertThrows(IllegalArgumentException.class,
                () -> servicio.listarPropias(sinId));

        Usuario otroRol = new Usuario("Juan", Rol.PACIENTE, 7L);
        assertThrows(IllegalArgumentException.class,
                () -> servicio.listarPropias(otroRol));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearPropia(MEDICO, null));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarPropia(MEDICO, 0));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.eliminarPropia(MEDICO, -1));
    }

    @Test
    void rechazaRespuestaJsonInvalida() {
        jsonRespuesta = "no-json";
        assertThrows(ServicioNoDisponibleException.class,
                () -> servicio.consultarPropia(MEDICO, 4L));
    }
}
